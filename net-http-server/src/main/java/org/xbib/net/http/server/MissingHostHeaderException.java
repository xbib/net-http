package org.xbib.net.http.server;

import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.route.HttpRouterContext;

@SuppressWarnings("serial")
public class MissingHostHeaderException extends HttpException {

    public MissingHostHeaderException(String message, HttpRouterContext httpRouterContext) {
        super(message, httpRouterContext, HttpResponseStatus.BAD_REQUEST);
    }
}
