package org.xbib.net.http.server.application;

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
import org.xbib.net.http.server.route.BaseHttpRouterContext;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.cookie.IncomingCookieHandler;
import org.xbib.net.http.server.cookie.OutgoingCookieHandler;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.render.HttpResponseRenderer;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.http.server.session.IncomingSessionHandler;
import org.xbib.net.http.server.session.OutgoingSessionHandler;
import org.xbib.net.http.server.session.PersistSessionHandler;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.http.server.session.memory.MemoryPropertiesSessionCodec;
import org.xbib.net.http.server.validate.HttpRequestValidator;
import org.xbib.net.mime.MimeTypeService;
import org.xbib.net.util.RandomUtil;
import org.xbib.settings.Settings;

public class BaseApplication implements Application {

    private static final Logger logger = Logger.getLogger(BaseApplication.class.getName());

    protected BaseApplicationBuilder builder;

    protected final String sessionName;

    private final HttpResponseRenderer httpResponseRenderer;

    protected List<ApplicationModule> applicationModuleList;

    protected BaseApplication(BaseApplicationBuilder builder) {
        this.builder = builder;
        this.sessionName = builder.settings.get("session.name", "SESS");
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
    public HttpRouterContext createContext(HttpDomain domain,
                                           HttpRequestBuilder requestBuilder,
                                           HttpResponseBuilder responseBuilder) {
        HttpRouterContext httpRouterContext = new BaseHttpRouterContext(this, domain, requestBuilder, responseBuilder);
        httpRouterContext.addOpenHandler(newRequestValidator());
        httpRouterContext.addOpenHandler(newIncomingCookieHandler());
        if (builder.sessionsEnabled) {
            Codec<Session> sessionCodec = newSessionCodec(httpRouterContext);
            httpRouterContext.getAttributes().put("sessioncodec", sessionCodec);
            httpRouterContext.addOpenHandler(newIncomingSessionHandler(sessionCodec));
            httpRouterContext.addCloseHandler(newOutgoingSessionHandler());
            httpRouterContext.addReleaseHandler(newPersistSessionHandler(sessionCodec));
        }
        httpRouterContext.addCloseHandler(newOutgoingCookieHandler());
        return httpRouterContext;
    }

    @Override
    public void releaseContext(HttpRouterContext httpRouterContext) {
        try {
            httpRouterContext.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    protected Codec<Session> newSessionCodec(HttpRouterContext httpRouterContext) {
        return new MemoryPropertiesSessionCodec(sessionName,this, 1024, Duration.ofDays(1));
    }

    protected HttpHandler newIncomingSessionHandler(Codec<Session> sessionCodec) {
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

    protected HttpHandler newOutgoingSessionHandler() {
        return new OutgoingSessionHandler(
                getSecret(),
                "HmacSHA1",
                sessionName,
                getStaticFileSuffixes(),
                "user_id",
                "e_user_id",
                Duration.ofDays(1),
                true,
                false,
                SameSite.LAX
        );
    }

    protected HttpHandler newPersistSessionHandler(Codec<Session> sessionCodec) {
        return new PersistSessionHandler(sessionCodec);
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
    public void onOpen(HttpRouterContext httpRouterContext) {
        try {
            // call modules after request/cookie/session setup
            applicationModuleList.forEach(module -> module.onOpen(httpRouterContext));
        } catch (Throwable t) {
            builder.httpRouter.routeToErrorHandler(httpRouterContext, t);
            httpRouterContext.fail();
        }
    }

    @Override
    public void onClose(HttpRouterContext httpRouterContext) {
        try {
            // call modules before session/cookie
            applicationModuleList.forEach(module -> module.onClose(httpRouterContext));
        } catch (Throwable t) {
            builder.httpRouter.routeToErrorHandler(httpRouterContext, t);
        } finally {
            try {
                if (httpResponseRenderer != null) {
                    httpResponseRenderer.handle(httpRouterContext);
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
        logger.log(Level.INFO, "application closed");
    }
}
