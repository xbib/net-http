package org.xbib.net.http.server;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.route.BaseHttpRouterContext;
import org.xbib.net.http.server.route.HttpRouterContext;

@SuppressWarnings("serial")
public class HttpException extends IOException {

    private final HttpRouterContext httpRouterContext;

    private final HttpResponseStatus httpResponseStatus;

    public HttpException(HttpResponseBuilder httpResponseBuilder,
                         HttpResponseStatus httpResponseStatus) {
        this(httpResponseStatus.codeAsText(),
                new BaseHttpRouterContext(null, null, null, httpResponseBuilder),
                httpResponseStatus);
    }

    public HttpException(String message,
                         HttpRouterContext httpRouterContext,
                         HttpResponseStatus httpResponseStatus) {
        super(message);
        this.httpRouterContext = httpRouterContext;
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpException(String message, Throwable throwable,
                         HttpRouterContext httpRouterContext,
                         HttpResponseStatus httpResponseStatus) {
        super(message, throwable);
        this.httpRouterContext = httpRouterContext;
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpException(Throwable throwable,
                         HttpRouterContext httpRouterContext,
                         HttpResponseStatus httpResponseStatus) {
        super(throwable);
        this.httpRouterContext = httpRouterContext;
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpRouterContext getHttpServerContext() {
        return httpRouterContext;
    }

    public HttpResponseStatus getResponseStatus() {
        return httpResponseStatus;
    }
}
