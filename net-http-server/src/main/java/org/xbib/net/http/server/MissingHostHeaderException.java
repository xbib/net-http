package org.xbib.net.http.server;

import org.xbib.net.http.HttpResponseStatus;

@SuppressWarnings("serial")
public class MissingHostHeaderException extends HttpException {

    public MissingHostHeaderException(String message, HttpServerContext httpServerContext) {
        super(message, httpServerContext, HttpResponseStatus.BAD_REQUEST);
    }
}
