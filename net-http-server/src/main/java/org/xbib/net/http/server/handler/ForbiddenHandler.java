package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.HttpServerContext;

public class ForbiddenHandler implements HttpErrorHandler {

    public ForbiddenHandler() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        context.response()
                .setResponseStatus(HttpResponseStatus.FORBIDDEN)
                .write("Forbidden");
    }
}
