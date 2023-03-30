package org.xbib.net.http.server.render;

import java.io.IOException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpResponse;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.HttpServerContext;

public class HttpResponseRenderer implements HttpHandler {

    public HttpResponseRenderer() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        HttpResponseBuilder httpResponseBuilder = context.response();
        // here we do the heavy lifting of rendering all elements for the response
        HttpResponse httpResponse = httpResponseBuilder.build();
        if (httpResponseBuilder.shouldClose()) {
            httpResponse.close();
        }
    }
}
