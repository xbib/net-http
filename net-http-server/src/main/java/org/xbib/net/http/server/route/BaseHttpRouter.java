package org.xbib.net.http.server.route;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.datastructures.common.LinkedHashSetMultiMap;
import org.xbib.datastructures.common.MultiMap;
import org.xbib.datastructures.json.tiny.Json;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.CookieBox;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.application.ApplicationModule;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.handler.InternalServerErrorHandler;
import org.xbib.net.http.server.service.HttpService;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;
import static org.xbib.net.http.HttpResponseStatus.NOT_FOUND;

public class BaseHttpRouter implements HttpRouter {

    private static final String PATH_SEPARATOR = "/";

    private final Logger logger = Logger.getLogger(BaseHttpRouter.class.getName());

    private final BaseHttpRouterBuilder builder;

    private final MultiMap<String, HttpDomain> domains;

    private final DomainsByAddress domainsByAddress;

    protected BaseHttpRouter(BaseHttpRouterBuilder builder) {
        this.builder = builder;
        this.domains = createDomains(builder.domains);
        this.domainsByAddress = createAddresses(builder.domains);
    }

    public static BaseHttpRouterBuilder builder() {
        return new BaseHttpRouterBuilder();
    }

    @Override
    public Collection<HttpDomain> getDomains() {
        return builder.domains;
    }

    @Override
    public DomainsByAddress getDomainsByAddress() {
        return domainsByAddress;
    }

    @Override
    public void route(Application application,
                      HttpRequestBuilder requestBuilder,
                      HttpResponseBuilder responseBuilder) {
        Objects.requireNonNull(requestBuilder);
        Objects.requireNonNull(requestBuilder.getRequestURI());
        Objects.requireNonNull(requestBuilder.getBaseURL());
        HttpDomain httpDomain = findDomain(requestBuilder.getBaseURL());
        if (httpDomain == null) {
            httpDomain = builder.domains.iterator().next();
        }
        List<HttpRouteResolver.Result<HttpService>> httpRouteResolverResults = new ArrayList<>();
        requestBuilder.setRequestPath(extractPath(requestBuilder.getRequestURI()));
        HttpRoute httpRoute = new BaseHttpRoute(httpDomain.getAddress(),
                Set.of(requestBuilder.getMethod()),
                builder.prefix,
                requestBuilder.getRequestPath(),
                true);
        builder.httpRouteResolver.resolve(httpRoute, httpRouteResolverResults::add);
        HttpRouterContext httpRouterContext = application.createContext(httpDomain,
                requestBuilder, responseBuilder);
        // before open: invoke security, incoming cookie/session
        httpRouterContext.getOpenHandlers().forEach(h -> {
            try {
                h.handle(httpRouterContext);
            } catch (Exception e) {
                routeToErrorHandler(httpRouterContext, e);
            }
        });
        application.onOpen(httpRouterContext);
        try {
            route(application, httpRouterContext, httpRouteResolverResults);
        } finally {
            httpRouterContext.getCloseHandlers().forEach(h -> {
                try {
                    h.handle(httpRouterContext);
                } catch (Exception e) {
                    routeToErrorHandler(httpRouterContext, e);
                }
            });
            application.onClose(httpRouterContext);
            httpRouterContext.getReleaseHandlers().forEach(h -> {
                try {
                    h.handle(httpRouterContext);
                } catch (Exception e) {
                    routeToErrorHandler(httpRouterContext, e);
                }
            });
            application.releaseContext(httpRouterContext);
        }
    }

    protected void route(Application application,
                         HttpRouterContext httpRouterContext,
                         List<HttpRouteResolver.Result<HttpService>> httpRouteResolverResults) {
        if (httpRouterContext.isFailed()) {
            return;
        }
        if (httpRouteResolverResults.isEmpty()) {
            logger.log(Level.FINE, "route resolver results is empty for " + httpRouterContext.getRequestBuilder().getRequestURI()
                    + ", generating a not found message");
            setResolverResult(httpRouterContext, null);
            routeStatus(NOT_FOUND, httpRouterContext);
            return;
        }
        for (HttpRouteResolver.Result<HttpService> httpRouteResolverResult : httpRouteResolverResults) {
            HttpService httpService = null;
            HttpRequest httpRequest = null;
            try {
                // first: create the final request
                setResolverResult(httpRouterContext, httpRouteResolverResult);
                httpService = httpRouteResolverResult.getValue();
                httpRequest = httpRouterContext.getRequest();
                for (ApplicationModule module : application.getModules()) {
                    module.onOpen(httpRouterContext, httpService, httpRequest);
                }
                // second: security check, authentication etc.
                if (httpService.getSecurityDomain() != null) {
                    logger.log(Level.FINEST, "handling security domain service " + httpService);
                    for (HttpHandler httpHandler : httpService.getSecurityDomain().getHandlers()) {
                        logger.log(Level.FINEST, () -> "handling security domain handler " + httpHandler);
                        httpHandler.handle(httpRouterContext);
                    }
                }
                if (httpRouterContext.isDone() || httpRouterContext.isFailed()) {
                    break;
                }
                // after security checks, accept service, open and execute service
                httpRouterContext.getAttributes().put("service", httpService);
                logger.log(Level.FINEST, "handling service " + httpService);
                httpService.handle(httpRouterContext);
                // if service signals that work is done, break
                if (httpRouterContext.isDone() || httpRouterContext.isFailed()) {
                    for (ApplicationModule module : application.getModules()) {
                        module.onSuccess(httpRouterContext, httpService, httpRequest);
                    }
                    break;
                }
                if (httpRouterContext.isFailed()) {
                    for (ApplicationModule module : application.getModules()) {
                        module.onFail(httpRouterContext, httpService, httpRequest, httpRouterContext.getFail());
                    }
                    break;
                }
            } catch (HttpException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                for (ApplicationModule module : application.getModules()) {
                    module.onFail(httpRouterContext, httpService, httpRequest, httpRouterContext.getFail());
                }
                routeException(e);
                break;
            } catch (Throwable t) {
                logger.log(Level.SEVERE, t.getMessage(), t);
                routeToErrorHandler(httpRouterContext, t);
                break;
            }
        }
    }

    protected void setResolverResult(HttpRouterContext httpRouterContext,
                                     HttpRouteResolver.Result<HttpService> pathResolverResult) {
        if (pathResolverResult != null) {
            httpRouterContext.getAttributes().put("context", pathResolverResult.getContext());
            httpRouterContext.getAttributes().put("handler", pathResolverResult.getValue());
            httpRouterContext.getAttributes().put("pathparams", pathResolverResult.getParameter());
            String contextPath = pathResolverResult.getContext() != null ?
                    PATH_SEPARATOR + String.join(PATH_SEPARATOR, pathResolverResult.getContext()) : null;
            httpRouterContext.setContextPath(contextPath);
            httpRouterContext.setContextURL(httpRouterContext.getRequestBuilder().getBaseURL().resolve(contextPath != null ? contextPath + "/" : ""));
        } else {
            // path resolver result null means "404 not found". Set default values.
            httpRouterContext.getAttributes().put("context", null);
            httpRouterContext.getAttributes().put("handler", null);
            httpRouterContext.getAttributes().put("pathparams", null);
            httpRouterContext.setContextPath(PATH_SEPARATOR);
            httpRouterContext.setContextURL(httpRouterContext.getRequestBuilder().getBaseURL());
        }
        HttpRequest httpRequest = createRequest(httpRouterContext, pathResolverResult);
        httpRouterContext.setRequest(httpRequest);
        httpRouterContext.getAttributes().put("request", httpRequest);
    }

    protected HttpRequest createRequest(HttpRouterContext httpRouterContext,
                                        HttpRouteResolver.Result<HttpService> pathResolverResult) {
        HttpRequestBuilder httpRequestBuilder = httpRouterContext.getRequestBuilder();
        HttpHeaders headers = httpRequestBuilder.getHeaders();
        String mimeType = headers.get(CONTENT_TYPE);
        Charset charset = StandardCharsets.UTF_8;
        if (mimeType != null) {
            charset = getCharset(mimeType, charset);
        }
        ParameterBuilder parameterBuilder = Parameter.builder()
                .domain(Parameter.Domain.QUERY)
                .charset(charset);
        // helper URL to collect parameters in request URI
        URL url = URL.builder()
                .charset(charset, CodingErrorAction.REPLACE)
                .path(httpRequestBuilder.getRequestURI())
                .build();
        ParameterBuilder formParameterBuilder = Parameter.builder()
                .domain(Parameter.Domain.FORM)
                .enableDuplicates();
        // https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4
        if (HttpMethod.POST.equals(httpRequestBuilder.getMethod()) &&
                (mimeType != null && mimeType.contains(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED))) {
            Charset htmlCharset = getCharset(mimeType, StandardCharsets.ISO_8859_1);
            CharBuffer charBuffer = httpRequestBuilder.getBodyAsChars(htmlCharset);
            if (charBuffer != null) {
                formParameterBuilder.addPercentEncodedBody(charBuffer.toString());
            }
        }
        String contentType = httpRequestBuilder.getHeaders().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType != null && contentType.contains(HttpHeaderValues.APPLICATION_JSON)) {
            String content = httpRequestBuilder.getBodyAsChars(StandardCharsets.UTF_8).toString();
            try {
                Map<String, Object> map = Json.toMap(content);
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof Iterable<?> iterable) {
                        iterable.forEach(it -> formParameterBuilder.add(entry.getKey(), it));
                    } else {
                        formParameterBuilder.add(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "unable to decode json body: " + e.getMessage(), e);
            }
        }
        CookieBox cookieBox = httpRouterContext.getAttributes().get(CookieBox.class, "incomingcookies");
        ParameterBuilder cookieParameterBuilder = Parameter.builder()
                .domain(Parameter.Domain.COOKIE);
        if (cookieBox != null) {
            cookieBox.forEach(c -> cookieParameterBuilder.add(c.name(), c.value()));
        }
        Parameter queryParameter = url.getQueryParams();
        parameterBuilder.add(queryParameter);
        Parameter formParameter = formParameterBuilder.build();
        parameterBuilder.add(formParameter);
        Parameter cookieParameter = cookieParameterBuilder.build();
        parameterBuilder.add(cookieParameter);
        if (pathResolverResult != null) {
            Parameter pathParameter = pathResolverResult.getParameter();
            parameterBuilder.add(pathParameter);
        }
        httpRequestBuilder.setParameter(parameterBuilder.build());
        httpRequestBuilder.setContext(httpRouterContext);
        return httpRequestBuilder.build();
    }

    private static Charset getCharset(String contentTypeValue, Charset defaultCharset) {
        if (contentTypeValue != null) {
            CharSequence charsetRaw = getCharsetAsSequence(contentTypeValue);
            if (charsetRaw != null) {
                if (charsetRaw.length() > 2) {
                    if (charsetRaw.charAt(0) == '"' && charsetRaw.charAt(charsetRaw.length() - 1) == '"') {
                        charsetRaw = charsetRaw.subSequence(1, charsetRaw.length() - 1);
                    }
                }
                try {
                    return Charset.forName(charsetRaw.toString());
                } catch (IllegalCharsetNameException | UnsupportedCharsetException ignored) {
                    // just return the default charset
                }
            }
        }
        return defaultCharset;
    }

    private static CharSequence getCharsetAsSequence(String contentTypeValue) {
        Objects.requireNonNull(contentTypeValue);
        int indexOfCharset = contentTypeValue.indexOf("charset=");
        if (indexOfCharset == -1) {
            return null;
        }
        int indexOfEncoding = indexOfCharset + "charset=".length();
        if (indexOfEncoding < contentTypeValue.length()) {
            CharSequence charsetCandidate = contentTypeValue.subSequence(indexOfEncoding, contentTypeValue.length());
            int indexOfSemicolon = charsetCandidate.toString().indexOf(";");
            if (indexOfSemicolon == -1) {
                return charsetCandidate;
            }
            return charsetCandidate.subSequence(0, indexOfSemicolon);
        }
        return null;
    }

    @Override
    public void routeException(HttpException e) {
        routeStatus(e.getResponseStatus(), e.getHttpServerContext());
    }

    @Override
    public void routeStatus(HttpResponseStatus httpResponseStatus,
                            HttpRouterContext httpRouterContext) {
        logger.log(Level.FINER, "routing status " + httpResponseStatus);
        try {
            HttpHandler httpHandler = getHandler(httpResponseStatus);
            if (httpHandler == null) {
                logger.log(Level.FINER, "handler for " + httpResponseStatus + " not present, using default error handler");
                httpHandler = new InternalServerErrorHandler();
            }
            httpRouterContext.reset();
            httpHandler.handle(httpRouterContext);
            httpRouterContext.done();
            logger.log(Level.FINER, "routing status " + httpResponseStatus + " done");
        } catch (IOException ioe) {
            throw new IllegalStateException("unable to route response status, reason: " + ioe.getMessage(), ioe);
        }
    }

    @Override
    public void routeToErrorHandler(HttpRouterContext httpRouterContext, Throwable t) {
        httpRouterContext.getAttributes().put("_throwable", t);
        httpRouterContext.fail(t);
        routeStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpRouterContext);
    }

    private HttpDomain findDomain(URL url) {
        NavigableSet<HttpDomain> httpDomains = new TreeSet<>();
        String hostAndPort = getHostAndPost(url);
        if (domains.containsKey(hostAndPort)) {
            httpDomains.addAll(domains.get(hostAndPort));
        }
        // check if ANY address was used for bind
        hostAndPort = "0.0.0.0:" + url.getPort();
        if (domains.containsKey(hostAndPort)) {
            httpDomains.addAll(domains.get(hostAndPort));
        }
        // check if IPv6 ANY address was used for bind
        hostAndPort = ":::" + url.getPort();
        if (domains.containsKey(hostAndPort)) {
            httpDomains.addAll(domains.get(hostAndPort));
        }
        return httpDomains.isEmpty() ? null: httpDomains.first();
    }

    private HttpHandler getHandler(HttpResponseStatus httpResponseStatus) {
        return builder.handlers.containsKey(httpResponseStatus.code()) ?
                builder.handlers.get(httpResponseStatus.code()) : builder.handlers.get(500);
    }

    private static MultiMap<String, HttpDomain> createDomains(Collection<HttpDomain> domains) {
        MultiMap<String, HttpDomain> map = new LinkedHashSetMultiMap<>();
        for (HttpDomain domain : domains) {
            HttpAddress httpAddress = domain.getAddress();
            if (httpAddress.getHostNames() != null) {
                for (String name : httpAddress.getHostNames()) {
                    map.put(name + ":" + httpAddress.getPort(), domain);
                }
            }
            for (String name : domain.getNames()) {
                map.put(name, domain);
            }
        }
        return map;
    }

    private static DomainsByAddress createAddresses(Collection<HttpDomain> domains) {
        DomainsByAddress map = new BaseDomainsByAddress();
        for (HttpDomain domain : domains) {
            map.put(domain.getAddress(), domain);
        }
        return map;
    }

    private static String extractPath(String uri) {
        String path = uri;
        int pos = uri.lastIndexOf('#');
        path = pos >= 0 ? path.substring(0, pos) : path;
        pos = uri.lastIndexOf('?');
        path = pos >= 0 ? path.substring(0, pos) : path;
        return path;
    }

    /**
     * Returns the host and port in notation "host:port" of the given URL.
     *
     * @param url the URL
     * @return the host and port
     */
    private static String getHostAndPost(URL url) {
        return url == null ? null : url.getPort() != null && url.getPort() != -1 ? url.getHost() + ":" + url.getPort() : url.getHost();
    }
}
