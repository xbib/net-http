package org.xbib.net.http.template.groovy;

import groovy.lang.Binding;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.HttpService;
import org.xbib.net.http.server.resource.negotiate.LocaleNegotiator;
import org.xbib.net.http.server.resource.HtmlTemplateResource;
import org.xbib.net.http.server.resource.HtmlTemplateResourceHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroovyTemplateResource extends HtmlTemplateResource {

    private static final Logger logger = Logger.getLogger(GroovyTemplateResource.class.getName());

    private static final Map<Path, Template> templates = new HashMap<>();

    private static final ReentrantLock lock = new ReentrantLock();

    protected GroovyTemplateResource(HtmlTemplateResourceHandler templateResourceHandler,
                                     HttpServerContext httpServerContext) throws IOException {
        super(templateResourceHandler, httpServerContext);
    }

    @Override
    public void render(HttpServerContext httpServerContext) throws IOException {
        logger.log(Level.FINE, "rendering groovy template, path = " + getPath() + " isExists = " + isExists() + " isDirectory =" + isDirectory() );
        Application application = httpServerContext.attributes().get(Application.class, "application");
        if (application == null) {
            logger.log(Level.WARNING, "application is null");
            return;
        }
        TemplateEngine templateEngine = httpServerContext.attributes().get(TemplateEngine.class, "templateengine");
        if (templateEngine == null) {
            logger.log(Level.WARNING, "template engine is null");
            return;
        }
        //
        Path templatePath = getPath();
        logger.log(Level.FINE, "templatePath = " + getPath());
        GroovyHttpResonseStatusTemplateResource resource = httpServerContext.attributes().get(GroovyHttpResonseStatusTemplateResource.class, "_resource");
        if (templatePath == null && isExists() && resource != null) {
            logger.log(Level.FINE, "Groovy HTTP status response rendering");
            String indexFileName = resource.getIndexFileName();
            if (indexFileName != null) {
                templatePath = application.resolve(indexFileName);
            }
            if (templatePath == null) {
                HttpService service = httpServerContext.attributes().get(HttpService.class, "service");
                GroovyTemplateService groovyTemplateService = (GroovyTemplateService) service;
                if (groovyTemplateService.getTemplateName() != null) {
                    templatePath = application.resolve(groovyTemplateService.getTemplateName());
                    logger.log(Level.FINE, "templatePath after application.resolve() = " + templatePath);
                } else {
                    logger.log(Level.FINE, "the GroovyTemplateService does not have a template name set");
                }
            }
        }
        // override if 'templatePath' attribute is set
        String overridePath = httpServerContext.attributes().get(String.class, "templatePath");
        if (overridePath != null) {
            logger.log(Level.FINE, "found override templatePath = " + overridePath);
            templatePath = application.resolve(overridePath);
            logger.log(Level.FINE, "found override templatePath, resolved to " + templatePath);
        }
        if (templatePath == null) {
            logger.log(Level.FINE, "templatePath is null, OOTB effort on " + getIndexFileName());
            // OOTB rendering via getIndexFileName(), no getPath(), no getTemplateName()
            templatePath = application.resolve(getIndexFileName());
        }
        if (isDirectory()) {
            logger.log(Level.WARNING, "unable to render a directory, this is forbidden");
            throw new HttpException("forbidden", httpServerContext, HttpResponseStatus.FORBIDDEN);
        }
        logger.log(Level.FINE, "rendering groovy template " + templatePath);
        templates.computeIfAbsent(templatePath, path -> {
            try {
                return templateEngine.createTemplate(Files.readString(path));
            } catch (ClassNotFoundException | IOException e) {
                throw new IllegalArgumentException(e);
            }
        });
        Template template = templates.get(templatePath);
        Logger templateLogger = Logger.getLogger("template." + getName().replace('/', '.'));
        Binding binding = new Binding();
        binding.setVariable("variables", binding.getVariables());
        httpServerContext.attributes().forEach(binding::setVariable);
        binding.setVariable("logger", templateLogger);
        binding.setVariable("log", templateLogger);
        DefaultTemplateResolver templateResolver = httpServerContext.attributes().get(DefaultTemplateResolver.class, "templateresolver");
        if (templateResolver != null) {
            // handle programmatic locale change plus template making under lock so no other request/response can interrupt us
            logger.log(Level.FINER, "application locale for template = " + application.getLocale());
            try {
                lock.lock();
                templateResolver.setLocale(application.getLocale());
                String acceptLanguage = httpServerContext.request().getHeaders().get(HttpHeaderNames.ACCEPT_LANGUAGE);
                if (acceptLanguage != null) {
                    Locale negotiatedLocale = LocaleNegotiator.findLocale(acceptLanguage);
                    if (negotiatedLocale != null) {
                        logger.log(Level.FINER, "negotiated locale for template = " + negotiatedLocale);
                        templateResolver.setLocale(negotiatedLocale);
                    }
                }
                Writable writable = template.make(binding.getVariables());
                httpServerContext.attributes().put("writable", writable);
                httpServerContext.done();
            } catch (Exception e) {
                // in case there is not template with negotiated locale
                templateResolver.setLocale(application.getLocale());
                Writable writable = template.make(binding.getVariables());
                httpServerContext.attributes().put("writable", writable);
                httpServerContext.done();
            } finally {
                lock.unlock();
            }
        } else {
            // for Groovy template engines without a resolver
            Writable writable = template.make(binding.getVariables());
            httpServerContext.attributes().put("writable", writable);
            httpServerContext.done();
        }
        logger.log(Level.FINER, "rendering done: " + httpServerContext.isDone());
    }
}
