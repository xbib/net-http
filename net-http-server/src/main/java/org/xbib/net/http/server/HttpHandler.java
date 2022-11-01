package org.xbib.net.http.server;

import java.io.IOException;

@FunctionalInterface
public interface HttpHandler {

    void handle(HttpServerContext httpServerContext) throws IOException;
}
