package org.xbib.net.http.template.groovy;

import org.xbib.net.http.server.BaseApplicationModule;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.xbib.net.http.server.HttpService;

public class GroovyTemplateApplicationModule extends BaseApplicationModule {

    private GroovyMarkupTemplateHandler groovyMarkupTemplateHandler;

    private GroovyTemplateRenderer groovyTemplateRenderer;

    public GroovyTemplateApplicationModule() {
    }

    @Override
    public String getName() {
        return "groovy-template";
    }

    @Override
    public void onOpen(Application application) {
        this.groovyMarkupTemplateHandler = new GroovyMarkupTemplateHandler(application);
        this.groovyTemplateRenderer = new GroovyTemplateRenderer();
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext) {
        try {
            groovyMarkupTemplateHandler.handle(httpServerContext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest) {
        httpServerContext.attributes().put("params", httpRequest.getParameter().asSingleValuedMap());
    }

    @Override
    public void onClose(Application application, HttpServerContext httpServerContext) {
        try {
            groovyTemplateRenderer.handle(httpServerContext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
