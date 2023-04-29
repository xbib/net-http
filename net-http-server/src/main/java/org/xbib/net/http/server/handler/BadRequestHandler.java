package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class BadRequestHandler implements HttpErrorHandler {

    public BadRequestHandler() {
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        context.status(HttpResponseStatus.BAD_REQUEST)
                .header(CONTENT_TYPE, "text/plain;charset=utf-8")
                .body("Bad request")
                .done();
    }
}
