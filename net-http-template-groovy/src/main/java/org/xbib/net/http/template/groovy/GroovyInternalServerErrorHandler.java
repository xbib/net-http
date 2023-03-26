package org.xbib.net.http.template.groovy;

import org.xbib.net.Resource;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroovyInternalServerErrorHandler extends GroovyTemplateResourceHandler {

    private static final Logger logger = Logger.getLogger(GroovyInternalServerErrorHandler.class.getName());

    private final String templateName;

    public GroovyInternalServerErrorHandler(String templateName) {
        this.templateName = templateName;
    }

    @Override
    protected Resource createResource(HttpServerContext httpServerContext) throws IOException {
        return new GroovyHttpResonseStatusTemplateResource(this, httpServerContext, templateName,
                HttpResponseStatus.INTERNAL_SERVER_ERROR, createMessage(httpServerContext));
    }

    private String createMessage(HttpServerContext context) throws IOException {
        Throwable throwable = context.getAttributes().get(Throwable.class, "_throwable");
        if (throwable != null) {
            logger.log(Level.SEVERE, throwable.getMessage(), throwable);
        }
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            return httpException.getMessage();
        } else {
            return throwable != null ? throwable.getMessage() : "";
        }
    }
}
