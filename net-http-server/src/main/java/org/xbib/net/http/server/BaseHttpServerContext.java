package org.xbib.net.http.server;

import org.xbib.net.Attributes;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.cookie.CookieBox;
import org.xbib.net.http.server.route.HttpRouteResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class BaseHttpServerContext implements HttpServerContext {

    private static final Logger logger = Logger.getLogger(BaseHttpServerContext.class.getName());

    private static final String PATH_SEPARATOR = "/";

    private final Application application;

    private final HttpRequestBuilder httpRequestBuilder;

    private final HttpResponseBuilder httpResponseBuilder;

    private HttpRouteResolver.Result<HttpService> pathResolverResult;

    private String contextPath;

    private URL contextURL;

    private Attributes attributes;

    private HttpRequest httpRequest;

    private boolean done;

    private boolean failed;

    private boolean next;

    public BaseHttpServerContext(Application application,
                                 HttpDomain domain,
                                 HttpRequestBuilder httpRequestBuilder,
                                 HttpResponseBuilder httpResponseBuilder) {
        this.application = application;
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpResponseBuilder = httpResponseBuilder;
        this.attributes = new BaseAttributes();
        this.attributes.put("application", application);
        this.attributes.put("domain", domain);
        this.attributes.put("requestbuilder", httpRequestBuilder);
        this.attributes.put("responsebuilder", httpResponseBuilder);
    }

    @Override
    public HttpRequestBuilder request() {
        return httpRequestBuilder;
    }

    @Override
    public HttpResponseBuilder response() {
        return httpResponseBuilder;
    }

    @Override
    public HttpRequest httpRequest() {
        return httpRequest;
    }

    @Override
    public void setResolverResult(HttpRouteResolver.Result<HttpService> pathResolverResult) {
        this.pathResolverResult = pathResolverResult;
        if (pathResolverResult != null) {
            attributes.put("context", pathResolverResult.getContext());
            attributes.put("handler", pathResolverResult.getValue());
            attributes.put("pathparams", pathResolverResult.getParameter());
            String contextPath = pathResolverResult.getContext() != null ?
                    PATH_SEPARATOR + String.join(PATH_SEPARATOR, pathResolverResult.getContext()) : null;
            setContextPath(contextPath);
            setContextURL(request().getBaseURL().resolve(contextPath != null ? contextPath + "/" : ""));
        } else {
            // path resolver result null means "404 not found". Set default values.
            attributes.put("context", null);
            attributes.put("handler", null);
            attributes.put("pathparams", null);
            setContextPath(PATH_SEPARATOR);
            setContextURL(request().getBaseURL());
        }
        httpRequest = createRequest();
        attributes.put("request", httpRequest);
        next = false;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    public void setContextURL(URL contextURL) {
        this.contextURL = contextURL;
    }

    @Override
    public URL getContextURL() {
        return contextURL;
    }

    @Override
    public Path resolve(String path) {
        return application.resolve(path);
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public Attributes attributes() {
        return attributes;
    }

    @Override
    public void done() {
        this.done = true;
        this.httpRequestBuilder.done();
        this.httpResponseBuilder.done();
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public void fail() {
        this.failed = true;
    }

    public void next() {
        this.next = true;
    }

    public boolean isNext() {
        return next;
    }

    @Override
    public void write() throws IOException {
        httpResponseBuilder.write("");
    }

    @Override
    public void write(String string) throws IOException {
        httpResponseBuilder.write(string);
    }

    @Override
    public void write(CharBuffer charBuffer, Charset charset) throws IOException {
        httpResponseBuilder.write(charBuffer, charset);
    }

    @Override
    public void write(DataBuffer dataBuffer) throws IOException {
        httpResponseBuilder.write(dataBuffer);
    }

    @Override
    public void write(InputStream inputStream, int bufferSize) throws IOException {
        httpResponseBuilder.write(inputStream, bufferSize);
    }

    @Override
    public void write(FileChannel fileChannel, int bufferSize) throws IOException {
        httpResponseBuilder.write(fileChannel, bufferSize);
    }

    protected HttpRequest createRequest() {
        HttpHeaders headers = httpRequestBuilder.getHeaders();
        String mimeType = headers.get(CONTENT_TYPE);
        Charset charset = StandardCharsets.UTF_8;
        if (mimeType != null) {
            charset = getCharset(mimeType, charset);
        }
        ParameterBuilder parameterBuilder = Parameter.builder().charset(charset);
        // helper URL to collect parameters in request URI
        URL url = URL.builder()
                .charset(charset, CodingErrorAction.REPLACE)
                .path(httpRequestBuilder.getRequestURI())
                .build();
        ParameterBuilder formParameterBuilder = Parameter.builder().domain(Parameter.Domain.FORM);
        // https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4
        if (HttpMethod.POST.equals(httpRequestBuilder.getMethod()) &&
                (mimeType != null && mimeType.contains(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED))) {
            Charset htmlCharset = getCharset(mimeType, StandardCharsets.ISO_8859_1);
            CharBuffer charBuffer = httpRequestBuilder.getBodyAsChars(htmlCharset);
            if (charBuffer != null) {
                formParameterBuilder.addPercentEncodedBody(charBuffer.toString());
            }
        }
        CookieBox cookieBox = attributes.get(CookieBox.class, "incomingcookies");
        ParameterBuilder cookieParameterBuilder = Parameter.builder().domain(Parameter.Domain.COOKIE);
        if (cookieBox != null) {
            cookieBox.forEach(c -> cookieParameterBuilder.add(c.name(), c.value()));
        }
        Parameter queryParameter = url.getQueryParams();
        logger.log(Level.FINER, "adding query parameters = " + queryParameter.getDomain() + " " + queryParameter.allToString());
        parameterBuilder.add(queryParameter);
        Parameter formParameter = formParameterBuilder.build();
        logger.log(Level.FINER, "adding form parameters = " + formParameter.getDomain() + " " + formParameter.allToString());
        parameterBuilder.add(formParameter);
        Parameter cookieParameter = cookieParameterBuilder.build();
        logger.log(Level.FINER, "adding cookie parameters = " + cookieParameter.getDomain() + " " + cookieParameter.allToString());
        parameterBuilder.add(cookieParameter);
        if (pathResolverResult != null) {
            Parameter pathParameter = pathResolverResult.getParameter();
            logger.log(Level.FINER, "adding path parameters = " + pathParameter.getDomain() + " " + pathParameter.allToString());
            parameterBuilder.add(pathParameter);
        }
        httpRequestBuilder.setParameter(parameterBuilder.build());
        httpRequestBuilder.setContext(this);
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

    // user session

    // request attributes

    // locale

    // principal

    // parsed form data, multipart

}
