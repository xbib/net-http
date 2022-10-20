package org.xbib.net.http.server;

import org.xbib.net.http.HttpResponseStatus;

@SuppressWarnings("serial")
public class UnknownExpectException extends HttpException {

    public UnknownExpectException(String message, HttpServerContext httpServerContext) {
        super(message, httpServerContext, HttpResponseStatus.EXPECTATION_FAILED);
    }
}
