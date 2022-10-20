package org.xbib.net.http.template.groovy;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.URL;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.nio.file.Path;

class GroovyHttpResonseStatusTemplateResource extends GroovyTemplateResource {

    private static final Logger logger = Logger.getLogger(GroovyHttpResonseStatusTemplateResource.class.getName());

    private final String indexFileName;

    private final HttpResponseStatus httpResponseStatus;

    private final String message;

    protected GroovyHttpResonseStatusTemplateResource(GroovyTemplateResourceHandler handler,
                                                      HttpServerContext httpServerContext,
                                                      String indexFileName,
                                                      HttpResponseStatus httpResponseStatus,
                                                      String message) throws IOException {
        super(handler, httpServerContext);
        this.indexFileName = indexFileName;
        this.httpResponseStatus = httpResponseStatus;
        this.message = message;
    }

    @Override
    public void render(HttpServerContext httpServerContext) throws IOException {
        logger.log(Level.FINE, "rendering HTTP status by Groovy");
        httpServerContext.attributes().put("_status", httpResponseStatus);
        httpServerContext.attributes().put("_message", message);
        httpServerContext.attributes().put("_resource", this);
        Application application = httpServerContext.attributes().get(Application.class, "application");
        GroovyMarkupTemplateHandler groovyMarkupTemplateHandler = new GroovyMarkupTemplateHandler(application);
        logger.log(Level.FINE, "handle groovyMarkupTemplateHandler");
        groovyMarkupTemplateHandler.handle(httpServerContext);
        super.render(httpServerContext);
        GroovyTemplateRenderer groovyTemplateRenderer = new GroovyTemplateRenderer();
        logger.log(Level.FINE, "handle groovyTemplateRenderer");
        groovyTemplateRenderer.handle(httpServerContext);
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public String getName() {
        return "status-resource";
    }

    @Override
    public String getBaseName() {
        return null;
    }

    @Override
    public String getSuffix() {
        return null;
    }

    @Override
    public String getResourcePath() {
        return null;
    }

    @Override
    public URL getURL() {
        return null;
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
