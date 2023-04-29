package org.xbib.net.http.server;

import org.xbib.net.http.server.route.HttpRouterContext;

import java.io.IOException;

@FunctionalInterface
public interface HttpHandler {

    void handle(HttpRouterContext httpRouterContext) throws IOException;
}
