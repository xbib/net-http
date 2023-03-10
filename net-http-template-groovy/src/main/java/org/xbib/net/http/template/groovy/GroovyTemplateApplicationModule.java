package org.xbib.net.http.template.groovy;

import groovy.text.markup.BaseTemplate;
import org.xbib.net.http.server.BaseApplicationModule;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xbib.net.http.server.HttpService;
import org.xbib.settings.Settings;

public class GroovyTemplateApplicationModule extends BaseApplicationModule {

    private static final Logger logger = Logger.getLogger(GroovyTemplateApplicationModule.class.getName());

    private GroovyMarkupTemplateHandler groovyMarkupTemplateHandler;

    private GroovyTemplateRenderer groovyTemplateRenderer;

    public GroovyTemplateApplicationModule(Application application, String name, Settings settings) {
        super(application, name, settings);
    }

    @Override
    public void onOpen(Application application, Settings settings) {
        ClassLoader classLoader = GroovyMarkupTemplateHandler.class.getClassLoader();
        String defaultMarkupTemplate = settings.get("markup.templateClass",
                "org.xbib.net.http.template.DefaultMarkupTemplate");
        try {
            @SuppressWarnings("unchecked")
            Class<? extends BaseTemplate> defaultMarkupTemplateClass =
                    (Class<? extends BaseTemplate>) Class.forName(defaultMarkupTemplate, true, classLoader);
            logger.log(Level.INFO, "markup template class = " + defaultMarkupTemplateClass.getName());
            this.groovyMarkupTemplateHandler = new GroovyMarkupTemplateHandler(application,
                    classLoader, defaultMarkupTemplateClass, application.getLocale(),
                    settings.getAsBoolean("markup.autoEscape", true),
                    settings.getAsBoolean("markup.autoIndent", false),
                    settings.get("markup.autoIndentString", "  "),
                    settings.getAsBoolean("markup.autoNewline", false),
                    settings.getAsBoolean("markup.cacheTemplates", true),
                    settings.get("markup.declarationEncoding", null),
                    settings.getAsBoolean("markup.expandEmptyElements", true),
                    settings.get("markup.newLine", System.getProperty("line.separator")),
                    settings.getAsBoolean("markup.useDoubleQuotes", true));
            this.groovyTemplateRenderer = new GroovyTemplateRenderer();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
        httpServerContext.attributes().put("request", httpRequest);
        httpServerContext.attributes().put("params", httpRequest.getParameter().asSingleValuedMap());
        application.getModules().forEach(module -> httpServerContext.attributes().put(module.getName(), module));
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
