package org.xbib.net.http.server.render;

import java.io.IOException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpResponse;
import org.xbib.net.http.server.HttpServerContext;

public class HttpResponseRenderer implements HttpHandler {

    public HttpResponseRenderer() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        // here we do the heavy lifting of rendering all elements for the response
        HttpResponse httpResponse = context.response().build();
        httpResponse.flush();
    }
}
