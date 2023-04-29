package org.xbib.net.http.server.handler;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.route.HttpRouterContext;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class InternalServerErrorHandler implements HttpErrorHandler {

    private static final Logger logger = Logger.getLogger(InternalServerErrorHandler.class.getName());

    public InternalServerErrorHandler() {
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        Throwable throwable = context.getAttributes().get(Throwable.class, "_throwable");
        if (throwable != null) {
            logger.log(Level.SEVERE, throwable.getMessage(), throwable);
        }
        HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        String message;
        if (throwable instanceof HttpException httpException) {
            status = httpException.getResponseStatus();
            message = httpException.getMessage();
        } else {
            message = throwable != null ? throwable.getMessage() : "";
        }
        context.status(status)
                .header(CONTENT_TYPE, "text/plain;charset=utf-8")
                .body(message)
                .done();
    }
}
