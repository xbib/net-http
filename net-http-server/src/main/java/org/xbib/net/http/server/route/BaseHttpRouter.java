package org.xbib.net.http.server.route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TreeSet;

import org.xbib.datastructures.common.LinkedHashSetMultiMap;
import org.xbib.datastructures.common.MultiMap;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpDomain;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.HttpService;
import org.xbib.net.http.server.handler.InternalServerErrorHandler;

import static org.xbib.net.http.HttpResponseStatus.NOT_FOUND;

public class BaseHttpRouter implements HttpRouter {

    private final Logger logger = Logger.getLogger(BaseHttpRouter.class.getName());

    private final BaseHttpRouterBuilder builder;

    private final MultiMap<String, HttpDomain> domains;

    private final DomainsByAddress domainsByAddress;

    private final HttpRouteResolver<HttpService> httpRouteResolver;

    private Application application;

    protected BaseHttpRouter(BaseHttpRouterBuilder builder) {
        this.builder = builder;
        HttpRouteResolver.Builder<HttpService> httpRouteResolverBuilder = newHttpRouteResolverBuilder();
        for (HttpDomain domain : builder.domains) {
            for (HttpService httpService : domain.getServices()) {
                logger.log(Level.FINER, "adding " + domain.getAddress() + " " + httpService.getMethods() +
                        " prefix = " + httpService.getPrefix() +
                        " path = " + httpService.getPathSpecification() + " " + httpService);
                HttpRoute httpRoute = new BaseHttpRoute(domain.getAddress(),
                        httpService.getMethods(),
                        httpService.getPrefix(),
                        httpService.getPathSpecification(), false);
                httpRouteResolverBuilder.add(httpRoute, httpService);
            }
        }
        this.httpRouteResolver = httpRouteResolverBuilder.build();
        this.domains = createDomains(builder.domains);
        this.domainsByAddress = createAddresses(builder.domains);
    }

    public static BaseHttpRouterBuilder builder() {
        return new BaseHttpRouterBuilder();
    }

    public HttpRouteResolver.Builder<HttpService> newHttpRouteResolverBuilder() {
        return BaseHttpRouteResolver.builder();
    }

    @Override
    public void setApplication(Application application) {
        this.application = application;
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
    public void route(HttpRequestBuilder requestBuilder, HttpResponseBuilder responseBuilder) {
        Objects.requireNonNull(application);
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
                "",
                requestBuilder.getRequestPath(),
                true);
        httpRouteResolver.resolve(httpRoute, httpRouteResolverResults::add);
        HttpServerContext httpServerContext = application.createContext(httpDomain, requestBuilder, responseBuilder);
        route(httpServerContext, httpRouteResolverResults);
    }

    protected void route(HttpServerContext httpServerContext, List<HttpRouteResolver.Result<HttpService>> httpRouteResolverResults) {
        application.onOpen(httpServerContext);
        try {
            if (httpServerContext.isFailed()) {
                return;
            }
            if (httpRouteResolverResults.isEmpty()) {
                logger.log(Level.FINE, "route resolver results is empty, generating a not found message");
                httpServerContext.setResolverResult(null);
                routeStatus(NOT_FOUND, httpServerContext);
                return;
            }
            for (HttpRouteResolver.Result<HttpService> httpRouteResolverResult : httpRouteResolverResults) {
                try {
                    // first: create the final request
                    httpServerContext.setResolverResult(httpRouteResolverResult);
                    HttpService httpService = httpRouteResolverResult.getValue();
                    HttpRequest httpRequest = httpServerContext.httpRequest();
                    application.getModules().forEach(module -> module.onOpen(application, httpServerContext, httpService, httpRequest));
                    // second: security check, authentication etc.
                    if (httpService.getSecurityDomain() != null) {
                        logger.log(Level.FINEST, () -> "handling security domain service " + httpService);
                        for (HttpHandler httpHandler : httpService.getSecurityDomain().getHandlers()) {
                            logger.log(Level.FINEST, () -> "handling security domain handler " + httpHandler);
                            httpHandler.handle(httpServerContext);
                        }
                    }
                    if (httpServerContext.isDone() || httpServerContext.isFailed()) {
                        break;
                    }
                    // after security checks, accept service, open and execute service
                    httpServerContext.attributes().put("service", httpService);
                    application.getModules().forEach(module -> module.onOpen(application, httpServerContext, httpService));
                    logger.log(Level.FINEST, () -> "handling service " + httpService);
                    httpService.handle(httpServerContext);
                    // if service signals that work is done, break
                    if (httpServerContext.isDone() || httpServerContext.isFailed()) {
                        break;
                    }
                } catch (HttpException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    routeException(e);
                    break;
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, t.getMessage(), t);
                    routeToErrorHandler(httpServerContext, t);
                    break;
                }
            }
        } finally {
            application.onClose(httpServerContext);
        }
    }

    @Override
    public void routeException(HttpException e) {
        routeStatus(e.getResponseStatus(), e.getHttpServerContext());
    }

    @Override
    public void routeStatus(HttpResponseStatus httpResponseStatus, HttpServerContext httpServerContext) {
        logger.log(Level.FINER, "routing status " + httpResponseStatus);
        try {
            HttpHandler httpHandler = getHandler(httpResponseStatus);
            if (httpHandler == null) {
                logger.log(Level.FINER, "handler for " + httpResponseStatus + " not present, using default error handler");
                httpHandler = new InternalServerErrorHandler();
            }
            httpServerContext.response().reset();
            httpHandler.handle(httpServerContext);
            httpServerContext.done();
            logger.log(Level.FINER, "routing status " + httpResponseStatus + " done");
        } catch (IOException ioe) {
            throw new IllegalStateException("unable to route response status, reason: " + ioe.getMessage(), ioe);
        }
    }

    @Override
    public void routeToErrorHandler(HttpServerContext httpServerContext, Throwable t) {
        httpServerContext.attributes().put("_throwable", t);
        httpServerContext.fail();
        routeStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpServerContext);
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
