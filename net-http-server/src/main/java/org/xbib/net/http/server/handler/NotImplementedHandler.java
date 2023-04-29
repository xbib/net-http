package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class NotImplementedHandler implements HttpErrorHandler {

    public NotImplementedHandler() {
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        context.status(HttpResponseStatus.NOT_IMPLEMENTED)
                .header(CONTENT_TYPE, "text/plain;charset=utf-8")
                .body("Not implemented")
                .done();
    }
}
