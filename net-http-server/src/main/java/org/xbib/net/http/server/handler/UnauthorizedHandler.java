package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.HttpServerContext;

public class UnauthorizedHandler implements HttpErrorHandler {

    public UnauthorizedHandler() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        context.response()
                .setResponseStatus(HttpResponseStatus.UNAUTHORIZED)
                .write("Unauthorized");
    }
}
