package org.xbib.net.http.server.route;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xbib.net.Attributes;
import org.xbib.net.URL;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.buffer.DataBufferFactory;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.auth.BaseAttributes;
import org.xbib.net.http.server.domain.HttpDomain;

public class BaseHttpRouterContext implements HttpRouterContext {

    private static final Logger logger = Logger.getLogger(BaseHttpRouterContext.class.getName());

    private final Application application;

    private final HttpRequestBuilder httpRequestBuilder;

    private final HttpResponseBuilder httpResponseBuilder;

    private final Attributes attributes;

    private final List<HttpHandler> openHandlers;

    private final List<HttpHandler> closeHandlers;

    private String contextPath;

    private URL contextURL;

    private HttpRequest httpRequest;

    private boolean done;

    private boolean failed;

    private boolean next;

    public BaseHttpRouterContext(Application application,
                                 HttpDomain domain,
                                 HttpRequestBuilder httpRequestBuilder,
                                 HttpResponseBuilder httpResponseBuilder) {
        this.application = application;
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpResponseBuilder = httpResponseBuilder;
        this.openHandlers = new LinkedList<>();
        this.closeHandlers = new LinkedList<>();
        this.attributes = new BaseAttributes();
        this.attributes.put("application", application);
        this.attributes.put("domain", domain);
        this.attributes.put("requestbuilder", httpRequestBuilder);
        this.attributes.put("responsebuilder", httpResponseBuilder);
        this.attributes.put("ctx", this);
    }

    @Override
    public void addOpenHandler(HttpHandler handler) {
        this.openHandlers.add(handler);
    }

    @Override
    public List<HttpHandler> getOpenHandlers() {
        return openHandlers;
    }

    @Override
    public void addCloseHandler(HttpHandler handler) {
        this.closeHandlers.add(handler);
    }

    @Override
    public List<HttpHandler> getCloseHandlers() {
        return closeHandlers;
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public HttpRequestBuilder getRequestBuilder() {
        return httpRequestBuilder;
    }

    public HttpResponseBuilder getResponseBuilder() {
        return httpResponseBuilder;
    }

    @Override
    public HttpRequest getRequest() {
        return httpRequest;
    }

    @Override
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
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

    @Override
    public DataBufferFactory getDataBufferFactory() {
        return httpResponseBuilder.getDataBufferFactory();
    }

    @Override
    public BaseHttpRouterContext status(int statusCode) {
        httpResponseBuilder.setResponseStatus(HttpResponseStatus.valueOf(statusCode));
        return this;
    }

    @Override
    public BaseHttpRouterContext status(HttpResponseStatus httpResponseStatus) {
        httpResponseBuilder.setResponseStatus(httpResponseStatus);
        return this;
    }

    @Override
    public HttpResponseStatus status() {
        return httpResponseBuilder.getResponseStatus();
    }

    @Override
    public BaseHttpRouterContext charset(Charset charset) {
        httpResponseBuilder.setCharset(charset);
        return this;
    }

    @Override
    public Attributes getAttributes() {
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
    public void reset() {
        httpResponseBuilder.reset();
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
    public void setRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public BaseHttpRouterContext header(String name, String value) {
        httpResponseBuilder.addHeader(name, value);
        return this;
    }

    @Override
    public HttpRouterContext cookie(Cookie cookie) {
        httpResponseBuilder.addCookie(cookie);
        return this;
    }

    @Override
    public BaseHttpRouterContext body(String string) throws IOException {
        httpResponseBuilder.write(string);
        return this;
    }

    @Override
    public BaseHttpRouterContext body(CharBuffer charBuffer, Charset charset) throws IOException {
        httpResponseBuilder.write(charBuffer, charset);
        return this;
    }

    @Override
    public BaseHttpRouterContext body(DataBuffer dataBuffer) throws IOException {
        httpResponseBuilder.write(dataBuffer);
        return this;
    }

    @Override
    public BaseHttpRouterContext body(InputStream inputStream, int bufferSize) throws IOException {
        httpResponseBuilder.write(inputStream, bufferSize);
        return this;
    }

    @Override
    public BaseHttpRouterContext body(FileChannel fileChannel, int bufferSize) throws IOException {
        httpResponseBuilder.write(fileChannel, bufferSize);
        return this;
    }

    @Override
    public long lengthInBytes() {
        return httpResponseBuilder.getLength();
    }

    @Override
    public void flush() throws IOException {
        httpResponseBuilder.build().flush();
    }

    @Override
    public void close() throws IOException {
        for (HttpHandler httpHandler : openHandlers) {
            if (httpHandler instanceof Closeable) {
                logger.log(Level.FINE, "closing handler " + httpHandler);
                ((Closeable) httpHandler).close();
            }
        }
        for (HttpHandler httpHandler : closeHandlers) {
            if (httpHandler instanceof Closeable) {
                logger.log(Level.FINE, "closing handler " + httpHandler);
                ((Closeable) httpHandler).close();
            }
        }
    }
}
