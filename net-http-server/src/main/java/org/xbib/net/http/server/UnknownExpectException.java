package org.xbib.net.http.server;

import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.route.HttpRouterContext;

@SuppressWarnings("serial")
public class UnknownExpectException extends HttpException {

    public UnknownExpectException(String message, HttpRouterContext httpRouterContext) {
        super(message, httpRouterContext, HttpResponseStatus.EXPECTATION_FAILED);
    }
}
