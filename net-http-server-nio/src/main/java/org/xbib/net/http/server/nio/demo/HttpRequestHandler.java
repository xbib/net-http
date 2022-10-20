package org.xbib.net.http.server.nio.demo;

public interface HttpRequestHandler {
    void handle(HttpContext ctx);
}
