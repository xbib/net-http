package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class UnauthorizedHandler implements HttpErrorHandler {

    public UnauthorizedHandler() {
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        context.status(HttpResponseStatus.UNAUTHORIZED)
                .header(CONTENT_TYPE, "text/plain;charset=utf-8")
                .body("Unauthorized")
                .done();
    }
}
