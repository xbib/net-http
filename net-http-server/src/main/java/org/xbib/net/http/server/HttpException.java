package org.xbib.net.http.server;

import org.xbib.net.http.HttpResponseStatus;
import java.io.IOException;

@SuppressWarnings("serial")
public class HttpException extends IOException {

    private final HttpServerContext httpServerContext;

    private final HttpResponseStatus httpResponseStatus;

    public HttpException(HttpResponseBuilder httpResponseBuilder,
                         HttpResponseStatus httpResponseStatus) {
        this(httpResponseStatus.codeAsText(),
                new BaseHttpServerContext(null, null, null, httpResponseBuilder),
                httpResponseStatus);
    }

    public HttpException(String message,
                         HttpServerContext httpServerContext,
                         HttpResponseStatus httpResponseStatus) {
        super(message);
        this.httpServerContext = httpServerContext;
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpException(String message, Throwable throwable,
                         HttpServerContext httpServerContext,
                         HttpResponseStatus httpResponseStatus) {
        super(message, throwable);
        this.httpServerContext = httpServerContext;
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpException(Throwable throwable,
                         HttpServerContext httpServerContext,
                         HttpResponseStatus httpResponseStatus) {
        super(throwable);
        this.httpServerContext = httpServerContext;
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpServerContext getHttpServerContext() {
        return httpServerContext;
    }

    public HttpResponseStatus getResponseStatus() {
        return httpResponseStatus;
    }
}
