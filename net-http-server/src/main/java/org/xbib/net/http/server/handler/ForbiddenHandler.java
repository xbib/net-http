package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class ForbiddenHandler implements HttpErrorHandler {

    public ForbiddenHandler() {
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        context.status(HttpResponseStatus.FORBIDDEN)
                .header(CONTENT_TYPE, "text/plain;charset=utf-8")
                .body("Forbidden")
                .done();
    }
}
