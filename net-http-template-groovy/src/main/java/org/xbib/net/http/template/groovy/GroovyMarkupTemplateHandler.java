package org.xbib.net.http.template.groovy;

import groovy.text.TemplateEngine;
import groovy.text.markup.BaseTemplate;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.application.Resolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class GroovyMarkupTemplateHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(GroovyMarkupTemplateHandler.class.getName());

    private final Resolver<Path> resolver;

    private final ClassLoader classLoader;

    private final TemplateConfiguration templateConfiguration;

    private final DefaultTemplateResolver templateResolver;

    private final TemplateEngine templateEngine;

    public GroovyMarkupTemplateHandler(Application application) {
        this(application, GroovyMarkupTemplateHandler.class.getClassLoader(),
                DefaultMarkupTemplate.class, application.getLocale(), false, false,
                "  ", false, true, null,
                true, System.getProperty("line.separator"), true);
    }

    public GroovyMarkupTemplateHandler(Resolver<Path> resolver,
                                       ClassLoader classLoader,
                                       Class<? extends BaseTemplate> templateClass,
                                       Locale locale,
                                       boolean autoEscape,
                                       boolean autoIndent,
                                       String autoIndentString,
                                       boolean autoNewLine,
                                       boolean cacheTemplates,
                                       String declarationEncoding,
                                       boolean expandEmptyElements,
                                       String newLineString,
                                       boolean useDoubleQuotes) {
        this.resolver = resolver;
        this.classLoader = classLoader;
        this.templateConfiguration = createConfiguration(templateClass != null ? templateClass : DefaultMarkupTemplate.class,
                locale, autoEscape, autoIndent, autoIndentString, autoNewLine, cacheTemplates, declarationEncoding,
                expandEmptyElements, newLineString, useDoubleQuotes);
        this.templateResolver = createTemplateResolver();
        this.templateEngine = createEngine();
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        DefaultTemplateResolver templateResolver = context.getAttributes().get(DefaultTemplateResolver.class, "templateresolver");
        if (templateResolver == null) {
            context.getAttributes().put("templateresolver", this.templateResolver);
            logger.log(Level.FINER, "setting templateresolver " + this.templateResolver);
        }
        TemplateEngine templateEngine = context.getAttributes().get(TemplateEngine.class, "templateengine");
        if (templateEngine == null) {
            context.getAttributes().put("templateengine", this.templateEngine);
            logger.log(Level.FINER, "setting templateengine " + this.templateEngine);
        }
    }

    protected TemplateConfiguration createConfiguration(Class<? extends BaseTemplate> templateClass,
                                                        Locale locale,
                                                        boolean autoEscape,
                                                        boolean autoIndent,
                                                        String autoIndentString,
                                                        boolean autoNewLine,
                                                        boolean cacheTemplates,
                                                        String declarationEncoding,
                                                        boolean expandEmptyElements,
                                                        String newLineString,
                                                        boolean useDoubleQuotes) {
        TemplateConfiguration templateConfiguration = new TemplateConfiguration();
        templateConfiguration.setBaseTemplateClass(templateClass);
        templateConfiguration.setLocale(locale);
        templateConfiguration.setAutoEscape(autoEscape);
        templateConfiguration.setAutoIndent(autoIndent);
        templateConfiguration.setAutoIndentString(autoIndentString);
        templateConfiguration.setAutoNewLine(autoNewLine);
        templateConfiguration.setCacheTemplates(cacheTemplates);
        templateConfiguration.setDeclarationEncoding(declarationEncoding);
        templateConfiguration.setExpandEmptyElements(expandEmptyElements);
        templateConfiguration.setNewLineString(newLineString);
        templateConfiguration.setUseDoubleQuotes(useDoubleQuotes);
        return templateConfiguration;
    }

    protected DefaultTemplateResolver createTemplateResolver() {
        return new DefaultTemplateResolver(resolver);
    }

    protected TemplateEngine createEngine() {
        return new MarkupTemplateEngine(classLoader, templateConfiguration, templateResolver);
    }
}
