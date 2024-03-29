package org.xbib.net.http.template.groovy;

import groovy.lang.Binding;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.net.http.server.resource.negotiate.LocaleNegotiator;
import org.xbib.net.http.server.resource.HtmlTemplateResource;
import org.xbib.net.http.server.resource.HtmlTemplateResourceHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroovyTemplateResource extends HtmlTemplateResource {

    private static final Logger logger = Logger.getLogger(GroovyTemplateResource.class.getName());

    /**
     * This class might be reused by mtultiple handlers.
     * We use a concurrent hash map here because in case of "not found" error templating,
     * the "computeIfAbsent" can be called in a nested way, and would throw a concurrent modification exception.
     */
    private static final Map<Path, Template> templates = new ConcurrentHashMap<>();

    /**
     * This lock ensures that changing locale and rendering is executed in a pairwisely manner.
     */
    private static final ReentrantLock lock = new ReentrantLock();

    protected GroovyTemplateResource(HtmlTemplateResourceHandler templateResourceHandler,
                                     HttpRouterContext httpRouterContext) throws IOException {
        super(templateResourceHandler, httpRouterContext);
    }

    @Override
    public void render(HttpRouterContext httpRouterContext) throws IOException {
        logger.log(Level.FINEST, () -> "rendering groovy template, path = " + getPath() + " isExists = " + isExists() + " isDirectory =" + isDirectory() );
        Application application = httpRouterContext.getAttributes().get(Application.class, "application");
        if (application == null) {
            logger.log(Level.WARNING, "application is null");
            return;
        }
        TemplateEngine templateEngine = httpRouterContext.getAttributes().get(TemplateEngine.class, "templateengine");
        if (templateEngine == null) {
            logger.log(Level.WARNING, "template engine is null");
            return;
        }
        Path templatePath = getPath();
        HttpService service = httpRouterContext.getAttributes().get(HttpService.class, "service");
        if (service instanceof GroovyTemplateService groovyTemplateService) {
            if (groovyTemplateService.getTemplateName() != null) {
                templatePath = application.resolve(groovyTemplateService.getTemplateName());
                logger.log(Level.FINEST, "templatePath after application.resolve() = " + templatePath);
            } else {
                logger.log(Level.FINEST, "the GroovyTemplateService does not have a templateName");
            }
        }
        // status response handlers have priority
        GroovyHttpResonseStatusTemplateResource resource = httpRouterContext.getAttributes().get(GroovyHttpResonseStatusTemplateResource.class, "_resource");
        if (resource != null) {
            String indexFileName = resource.getIndexFileName();
            if (indexFileName != null) {
                templatePath = application.resolve(indexFileName);
            }
            logger.log(Level.FINEST, "rendering Groovy HTTP status response with templatePath = " + templatePath);
        } else {
            // override if 'templatePath' attribute is set
            String overridePath = httpRouterContext.getAttributes().get(String.class, "templatePath");
            if (overridePath != null) {
                logger.log(Level.FINEST, "found override templatePath = " + overridePath);
                templatePath = application.resolve(overridePath);
                logger.log(Level.FINEST, "found override templatePath, resolved to " + templatePath);
            }
            if (templatePath == null) {
                logger.log(Level.FINEST, "templatePath is null, OOTB effort on " + getIndexFileName());
                // OOTB rendering via getIndexFileName(), no getPath(), no getTemplateName()
                templatePath = application.resolve(getIndexFileName());
            }
        }
        if (isDirectory()) {
            if (isExistsIndexFile()) {
                templatePath = getPath().resolve(getIndexFileName());
            } else {
                logger.log(Level.WARNING, "unable to render a directory without index file name, this is forbidden");
                throw new HttpException("forbidden", httpRouterContext, HttpResponseStatus.FORBIDDEN);
            }
        }
        if (templatePath == null) {
            logger.log(Level.WARNING, "unable to render a null path");
            throw new HttpException("internal path error", httpRouterContext, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        templates.computeIfAbsent(templatePath, path -> {
            try {
                logger.log(Level.FINEST, () -> "groovy templatePath = " + path + " creating by template engine");
                return templateEngine.createTemplate(Files.readString(path));
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        });
        Template template = templates.get(templatePath);
        Logger templateLogger = Logger.getLogger("template." + getName().replace('/', '.'));
        Binding binding = new Binding();
        httpRouterContext.getAttributes().forEach(binding::setVariable);
        binding.setVariable("logger", templateLogger);
        binding.setVariable("log", templateLogger);
        application.getModules().forEach(m -> binding.setVariable(m.getName(), m));
        DefaultTemplateResolver templateResolver = httpRouterContext.getAttributes().get(DefaultTemplateResolver.class, "templateresolver");
        if (templateResolver == null) {
            // for Groovy template engines without a resolver, no need to set a locale
            Writable writable = template.make(binding.getVariables());
            httpRouterContext.getAttributes().put("writable", writable);
            return;
        }
        if (!negotiateLocale) {
            // if no locale negotiation configured, set always the applicaiton locale. This constant value never changes.
            templateResolver.setLocale(application.getLocale());
            Writable writable = template.make(binding.getVariables());
            httpRouterContext.getAttributes().put("writable", writable);
            return;
        }
        // handle programmatic locale change plus template making under lock so no other request/response can interrupt us
        logger.log(Level.FINEST, () -> "application locale for template = " + application.getLocale());
        try {
            lock.lock();
            templateResolver.setLocale(application.getLocale());
            // language from request overrides application locale
            String acceptLanguage = httpRouterContext.getRequestBuilder().getHeaders().get(HttpHeaderNames.ACCEPT_LANGUAGE);
            if (acceptLanguage != null) {
                Locale negotiatedLocale = LocaleNegotiator.findLocale(acceptLanguage);
                if (negotiatedLocale != null) {
                    logger.log(Level.FINEST, () -> "negotiated locale for template = " + negotiatedLocale);
                    templateResolver.setLocale(negotiatedLocale);
                }
            }
            Writable writable = template.make(binding.getVariables());
            httpRouterContext.getAttributes().put("writable", writable);
        } catch (Exception e) {
            // fail silently by ignoring negotation
            templateResolver.setLocale(application.getLocale());
            Writable writable = template.make(binding.getVariables());
            httpRouterContext.getAttributes().put("writable", writable);
        } finally {
            lock.unlock();
        }
    }
}
