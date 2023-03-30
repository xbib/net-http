package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.HttpServerContext;

public class NotImplementedHandler implements HttpErrorHandler {

    public NotImplementedHandler() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        context.response()
                .setResponseStatus(HttpResponseStatus.NOT_IMPLEMENTED)
                .setContentType("text/plain;charset=utf-8")
                .write("Not implemented");
    }
}
