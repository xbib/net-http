package org.xbib.net.http.server.handler;

import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InternalServerErrorHandler implements HttpErrorHandler {

    private static final Logger logger = Logger.getLogger(InternalServerErrorHandler.class.getName());

    public InternalServerErrorHandler() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        Throwable throwable = context.getAttributes().get(Throwable.class, "_throwable");
        if (throwable != null) {
            logger.log(Level.SEVERE, throwable.getMessage(), throwable);
        }
        HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        String message;
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            status = httpException.getResponseStatus();
            message = httpException.getMessage();
        } else {
            message = throwable != null ? throwable.getMessage() : "";
        }
        context.response()
                .setResponseStatus(status)
                .setContentType("text/plain;charset=utf-8")
                .write(message);
    }
}
