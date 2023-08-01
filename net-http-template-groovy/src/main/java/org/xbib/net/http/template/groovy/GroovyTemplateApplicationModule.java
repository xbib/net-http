package org.xbib.net.http.template.groovy;

import groovy.text.markup.BaseTemplate;
import org.xbib.net.ParameterException;
import org.xbib.net.http.server.application.BaseApplicationModule;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.route.HttpRouterContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xbib.net.http.server.service.HttpService;
import org.xbib.settings.Settings;

public class GroovyTemplateApplicationModule extends BaseApplicationModule {

    private static final Logger logger = Logger.getLogger(GroovyTemplateApplicationModule.class.getName());

    private final GroovyMarkupTemplateHandler groovyMarkupTemplateHandler;

    private final GroovyTemplateRenderer groovyTemplateRenderer;

    public GroovyTemplateApplicationModule(Application application, String name, Settings settings) {
        super(application, name, settings);
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
    public void onOpen(HttpRouterContext httpRouterContext) {
        try {
            groovyMarkupTemplateHandler.handle(httpRouterContext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onOpen(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest) {
        try {
            httpRouterContext.getAttributes().put("request", httpRequest);
            httpRouterContext.getAttributes().put("params", httpRequest.getParameter().asSingleValuedMap());
            application.getModules().forEach(module -> httpRouterContext.getAttributes().put(module.getName(), module));
        } catch (ParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onClose(HttpRouterContext httpRouterContext) {
        try {
            groovyTemplateRenderer.handle(httpRouterContext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
