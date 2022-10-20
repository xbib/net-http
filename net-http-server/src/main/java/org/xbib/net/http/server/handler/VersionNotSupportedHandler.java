package org.xbib.net.http.server.handler;

import java.io.IOException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.HttpServerContext;

public class VersionNotSupportedHandler implements HttpErrorHandler {

    public VersionNotSupportedHandler() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        context.response()
                .setResponseStatus(HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED)
                .setContentType("text/plain;charset=utf-8")
                .write("HTTP version not supported");
    }
}
