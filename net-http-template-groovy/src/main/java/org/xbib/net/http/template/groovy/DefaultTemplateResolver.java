package org.xbib.net.http.template.groovy;

import groovy.text.markup.MarkupTemplateEngine.TemplateResource;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;
import org.xbib.net.http.server.application.Resolver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultTemplateResolver implements TemplateResolver {

    private static final Logger logger = Logger.getLogger(DefaultTemplateResolver.class.getName());

    private final Resolver<Path> resolver;

    private TemplateConfiguration templateConfiguration;

    private Locale locale;

    public DefaultTemplateResolver(Resolver<Path> resolver) {
        this.resolver = resolver;
    }

    @Override
    public void configure(ClassLoader cl, TemplateConfiguration templateConfiguration) {
        this.templateConfiguration = templateConfiguration;
    }

    @Override
    public URL resolveTemplate(String templatePath) throws IOException {
        TemplateResource templateResource = TemplateResource.parse(templatePath);
        String languageTag = locale != null ?
                locale.toLanguageTag().replace("-", "_") :
                templateConfiguration.getLocale() != null ? templateConfiguration.getLocale().toLanguageTag().replace("-", "_") :
                null;
        String templateResourceString = languageTag != null ?
                templateResource.withLocale(languageTag).toString() :
                templateResource.toString();
        logger.log(Level.FINER, "template resource string = " + templateResourceString + " locale = " + locale
           + " templateConfiguration.getLocale() = " + templateConfiguration.getLocale() + " languageTag = " + languageTag);
        Path path = resolver.resolve(templateResourceString);
        return path.toUri().toURL();
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}
