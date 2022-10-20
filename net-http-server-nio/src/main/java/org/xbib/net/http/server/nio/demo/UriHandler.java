package org.xbib.net.http.server.nio.demo;

import java.io.IOException;

public class UriHandler implements HttpRequestHandler {

    public UriHandler() {
    }

    @Override
    public void handle(HttpContext ctx) {
        HttpRequest request = ctx.getRequest();
        String uri = request.getUri();
        String body = "{\"uri\":\"" + uri + "\"}";
        try {
            ctx.status("200").json(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
