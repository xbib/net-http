package org.xbib.net.http.server.application;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.cookie.SameSite;
import org.xbib.net.http.server.BaseHttpServerContext;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.cookie.IncomingCookieHandler;
import org.xbib.net.http.server.cookie.OutgoingCookieHandler;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.render.HttpResponseRenderer;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.http.server.session.IncomingSessionHandler;
import org.xbib.net.http.server.session.OutgoingSessionHandler;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.http.server.session.memory.MemoryPropertiesSessionCodec;
import org.xbib.net.http.server.validate.HttpRequestValidator;
import org.xbib.net.mime.MimeTypeService;
import org.xbib.net.util.RandomUtil;
import org.xbib.settings.Settings;

public class BaseApplication implements Application {

    private static final Logger logger = Logger.getLogger(BaseApplication.class.getName());

    protected BaseApplicationBuilder builder;

    private final HttpRequestValidator httpRequestValidator;

    protected final String sessionName;

    private final HttpHandler incomingCookieHandler;

    private final HttpHandler outgoingCookieHandler;

    private final HttpResponseRenderer httpResponseRenderer;

    private Codec<Session> sessionCodec;

    private HttpHandler incomingSessionHandler;

    private HttpHandler outgoingSessionHandler;

    protected List<ApplicationModule> applicationModuleList;

    protected BaseApplication(BaseApplicationBuilder builder) {
        this.builder = builder;
        this.sessionName = getSettings().get("session.name", "SESS");
        this.httpRequestValidator = newRequestValidator();
        this.incomingCookieHandler = newIncomingCookieHandler();
        this.outgoingCookieHandler = newOutgoingCookieHandler();
        this.httpResponseRenderer = newResponseRenderer();
        this.applicationModuleList = new ArrayList<>();
        for (Map.Entry<String, Settings> entry : builder.settings.getGroups("module").entrySet()) {
            String moduleName = entry.getKey();
            Settings moduleSettings = entry.getValue();
            if (moduleSettings.getAsBoolean("enabled", true)) {
                try {
                    String className = moduleSettings.get("class");
                    @SuppressWarnings("unchecked")
                    Class<ApplicationModule> clazz =
                            (Class<ApplicationModule>) Class.forName(className, true, builder.classLoader);
                    ApplicationModule applicationModule = clazz.getConstructor(Application.class, String.class, Settings.class)
                            .newInstance(this, moduleName, moduleSettings);
                    applicationModuleList.add(applicationModule);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                    throw new IllegalArgumentException("class not found or not loadable: " + e.getMessage());
                }
            } else {
                logger.log(Level.WARNING, "disabled module: " + moduleName);
            }
        }
    }

    public static BaseApplicationBuilder builder() {
        return new BaseApplicationBuilder();
    }

    @Override
    public void addModule(ApplicationModule applicationModule) {
        applicationModuleList.add(applicationModule);
    }

    @Override
    public Locale getLocale() {
        return builder.locale;
    }

    @Override
    public ZoneId getZoneId() {
        return builder.zoneId;
    }

    @Override
    public MimeTypeService getMimeService() {
        return builder.mimeTypeService;
    }

    @Override
    public Path getHome() {
        return builder.home;
    }

    @Override
    public String getContextPath() {
        return builder.contextPath;
    }

    @Override
    public Settings getSettings() {
        return builder.settings;
    }

    public String getSecret() {
        return builder.secret;
    }

    public Set<String> getStaticFileSuffixes() {
        return builder.staticFileSuffixes;
    }

    @Override
    public Collection<ApplicationModule> getModules() {
        return applicationModuleList;
    }

    @Override
    public Collection<HttpDomain> getDomains() {
        return builder.httpRouter.getDomains();
    }

    @Override
    public Set<HttpAddress> getAddresses() {
        return builder.httpRouter.getDomainsByAddress().keySet();
    }

    @Override
    public HttpServerContext createContext(HttpDomain domain,
                                           HttpRequestBuilder requestBuilder,
                                           HttpResponseBuilder responseBuilder) {
        HttpServerContext httpServerContext = new BaseHttpServerContext(this, domain, requestBuilder, responseBuilder);
        httpServerContext.getAttributes().put("requestbuilder", requestBuilder);
        httpServerContext.getAttributes().put("responsebuilder", responseBuilder);
        this.sessionCodec = newSessionCodec(httpServerContext);
        if (sessionCodec != null) {
            httpServerContext.getAttributes().put("sessioncodec", sessionCodec);
        }
        this.incomingSessionHandler = newIncomingSessionHandler(httpServerContext);
        this.outgoingSessionHandler = newOutgoingSessionHandler(httpServerContext);
        return httpServerContext;
    }

    protected HttpRequestValidator newRequestValidator() {
        return new HttpRequestValidator();
    }

    protected HttpHandler newIncomingCookieHandler() {
        return new IncomingCookieHandler();
    }

    protected HttpHandler newOutgoingCookieHandler() {
        return new OutgoingCookieHandler();
    }

    protected HttpResponseRenderer newResponseRenderer() {
        return new HttpResponseRenderer();
    }

    protected Codec<Session> newSessionCodec(HttpServerContext httpServerContext) {
        return new MemoryPropertiesSessionCodec(sessionName,this, 1024, Duration.ofDays(1));
    }

    protected HttpHandler newIncomingSessionHandler(HttpServerContext httpServerContext) {
        @SuppressWarnings("unchecked")
        Codec<Session> sessionCodec = httpServerContext.getAttributes().get(Codec.class, "sessioncodec");
        return new IncomingSessionHandler(
                getSecret(),
                "HmacSHA1",
                sessionName,
                sessionCodec,
                getStaticFileSuffixes(),
                "user_id",
                "e_user_id",
                () -> RandomUtil.randomString(16));
    }

    protected HttpHandler newOutgoingSessionHandler(HttpServerContext httpServerContext) {
        @SuppressWarnings("unchecked")
        Codec<Session> sessionCodec = httpServerContext.getAttributes().get(Codec.class, "sessioncodec");
        return new OutgoingSessionHandler(
                getSecret(),
                "HmacSHA1",
                sessionName,
                sessionCodec,
                getStaticFileSuffixes(),
                "user_id",
                "e_user_id",
                Duration.ofDays(1),
                true,
                false,
                SameSite.LAX
        );
    }

    @Override
    public void onCreated(Session session) {
        logger.log(Level.FINER, "session name = " + sessionName + " created = " + session);
        applicationModuleList.forEach(module -> module.onOpen(session));
    }

    @Override
    public void onDestroy(Session session) {
        logger.log(Level.FINER, "session name = " + sessionName + " destroyed = " + session);
        applicationModuleList.forEach(module -> module.onClose(session));
    }

    @Override
    public void onOpen(HttpServerContext httpServerContext) {
        try {
            if (httpRequestValidator != null) {
                httpRequestValidator.handle(httpServerContext);
            }
            if (incomingCookieHandler != null) {
                incomingCookieHandler.handle(httpServerContext);
            }
            if (builder.sessionsEnabled && incomingSessionHandler != null) {
                incomingSessionHandler.handle(httpServerContext);
            }
            // call modules after request/cookie/session setup
            applicationModuleList.forEach(module -> module.onOpen(httpServerContext));
        } catch (HttpException e) {
            builder.httpRouter.routeException(e);
            httpServerContext.fail();
        } catch (Throwable t) {
            builder.httpRouter.routeToErrorHandler(httpServerContext, t);
            httpServerContext.fail();
        }
    }

    @Override
    public void onClose(HttpServerContext httpServerContext) {
        try {
            // call modules before session/cookie
            applicationModuleList.forEach(module -> module.onClose(httpServerContext));
            if (builder.sessionsEnabled && outgoingSessionHandler != null) {
                outgoingSessionHandler.handle(httpServerContext);
            }
            if (outgoingCookieHandler != null) {
                outgoingCookieHandler.handle(httpServerContext);
            }
        } catch (HttpException e) {
            builder.httpRouter.routeException(e);
        } catch (Throwable t) {
            builder.httpRouter.routeToErrorHandler(httpServerContext, t);
        } finally {
            try {
                if (httpResponseRenderer != null) {
                    httpResponseRenderer.handle(httpServerContext);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    @Override
    public Executor getExecutor() {
        return builder.executor;
    }

    @Override
    public HttpRouter getRouter() {
        return builder.httpRouter;
    }

    @Override
    public Path resolve(String string) {
        if (string == null) {
            return builder.home;
        }
        try {
            Path p = builder.home.resolve(string);
            if (Files.exists(p) && Files.isReadable(p)) {
                return p;
            } else {
                logger.log(Level.WARNING, "unable to find path: " + p + " on home " + builder.home);
            }
        } catch (Exception e) {
            // ignore
        }
        throw new IllegalArgumentException("unable to resolve '" + string + "' on home '" + builder.home + "'");
    }

    @Override
    public void close() throws IOException {
        logger.log(Level.INFO, "application closing");
        builder.executor.shutdown();
        // stop dispatching and stop dispatched requests
        applicationModuleList.forEach(module -> {
            logger.log(Level.FINE, "application closing module " + module);
            module.onClose();
        });
        if (outgoingSessionHandler != null && (outgoingSessionHandler instanceof Closeable)) {
            logger.log(Level.FINE, "application closing outgoing session handler");
            ((Closeable) outgoingSessionHandler).close();
        }
        if (incomingSessionHandler != null && (incomingSessionHandler instanceof Closeable)) {
            logger.log(Level.FINE, "application closing incoming session handler");
            ((Closeable) incomingSessionHandler).close();
        }
        if (sessionCodec != null && sessionCodec instanceof Closeable) {
            logger.log(Level.FINE, "application closing session codec");
            ((Closeable) sessionCodec).close();
        }
        if (outgoingCookieHandler != null && (outgoingCookieHandler instanceof Closeable)) {
            logger.log(Level.FINE, "application closing outgoing cookie handler");
            ((Closeable) outgoingCookieHandler).close();
        }
        if (incomingCookieHandler != null && (incomingCookieHandler instanceof Closeable)) {
            logger.log(Level.FINE, "application closing incoming cookie handler");
            ((Closeable) incomingCookieHandler).close();
        }
        logger.log(Level.INFO, "application closed");
    }
}
