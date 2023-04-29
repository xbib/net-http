package org.xbib.net.http.server.render;

import java.io.IOException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

public class HttpResponseRenderer implements HttpHandler {

    public HttpResponseRenderer() {
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        context.flush();
    }
}
