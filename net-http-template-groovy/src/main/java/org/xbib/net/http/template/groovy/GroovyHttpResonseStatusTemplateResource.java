package org.xbib.net.http.template.groovy;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.route.HttpRouterContext;

import java.io.IOException;

class GroovyHttpResonseStatusTemplateResource extends GroovyTemplateResource {

    private static final Logger logger = Logger.getLogger(GroovyHttpResonseStatusTemplateResource.class.getName());

    private final String indexFileName;

    private final HttpResponseStatus httpResponseStatus;

    private final String message;

    protected GroovyHttpResonseStatusTemplateResource(GroovyTemplateResourceHandler handler,
                                                      HttpRouterContext httpRouterContext,
                                                      String indexFileName,
                                                      HttpResponseStatus httpResponseStatus,
                                                      String message) throws IOException {
        super(handler, httpRouterContext);
        this.indexFileName = indexFileName;
        this.httpResponseStatus = httpResponseStatus;
        this.message = message;
    }

    @Override
    public void render(HttpRouterContext httpRouterContext) throws IOException {
        logger.log(Level.FINEST, "rendering HTTP status by Groovy");
        httpRouterContext.getAttributes().put("_status", httpResponseStatus);
        httpRouterContext.getAttributes().put("_message", message);
        httpRouterContext.getAttributes().put("_resource", this);
        Application application = httpRouterContext.getAttributes().get(Application.class, "application");
        GroovyMarkupTemplateHandler groovyMarkupTemplateHandler = new GroovyMarkupTemplateHandler(application);
        logger.log(Level.FINEST, "handle groovyMarkupTemplateHandler");
        groovyMarkupTemplateHandler.handle(httpRouterContext);
        super.render(httpRouterContext);
        GroovyTemplateRenderer groovyTemplateRenderer = new GroovyTemplateRenderer();
        groovyTemplateRenderer.handle(httpRouterContext);
    }

    @Override
    public String getName() {
        return "status-resource";
    }

    @Override
    public boolean isExists() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String getMimeType() {
        return "text/html; charset=UTF-8";
    }

    @Override
    public String getIndexFileName() {
        return indexFileName;
    }
}
