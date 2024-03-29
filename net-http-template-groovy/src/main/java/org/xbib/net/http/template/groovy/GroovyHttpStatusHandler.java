package org.xbib.net.http.template.groovy;

import org.xbib.net.Resource;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpErrorHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

import java.io.IOException;

public class GroovyHttpStatusHandler extends GroovyTemplateResourceHandler implements HttpErrorHandler {

    private final HttpResponseStatus httpResponseStatus;

    private final String message;

    private final String templateName;

    public GroovyHttpStatusHandler(HttpResponseStatus httpResponseStatus,
                                   String message,
                                   String templateName) {
        this.httpResponseStatus = httpResponseStatus;
        this.message = message;
        this.templateName = templateName;
    }

    @Override
    protected Resource createResource(HttpRouterContext httpRouterContext) throws IOException {
        return new GroovyHttpResonseStatusTemplateResource(this, httpRouterContext,
                templateName, httpResponseStatus, message);
    }
}
