package org.xbib.net.http.server;

import org.xbib.datastructures.common.ImmutableSet;
import org.xbib.net.http.server.route.HttpRouter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseApplicationBuilder implements ApplicationBuilder {

    private static final Logger logger = Logger.getLogger(BaseApplicationBuilder.class.getName());

    protected int blockingThreadCount;

    protected int blockingQueueCount;

    protected Path home;

    protected String contextPath;

    protected String secret;

    protected boolean sessionsEnabled;

    protected HttpRouter router;

    protected Locale locale;

    protected ZoneId zoneId;

    protected List<ApplicationModule> applicationModuleList;

    protected Set<String> staticFileSuffixes;

    protected BaseApplicationBuilder() {
        this.blockingThreadCount = Runtime.getRuntime().availableProcessors();
        this.blockingQueueCount = Integer.MAX_VALUE;
        this.home = Paths.get(System.getProperties().containsKey("application.home") ? System.getProperty("application.home") : ".");
        this.contextPath = "/";
        this.secret = "secret";
        this.sessionsEnabled = true;
        this.locale = Locale.getDefault();
        this.zoneId = ZoneId.systemDefault();
        this.applicationModuleList = new ArrayList<>();
    }

    @Override
    public BaseApplicationBuilder setThreadCount(int blockingThreadCount) {
        this.blockingThreadCount = blockingThreadCount;
        return this;
    }

    @Override
    public BaseApplicationBuilder setQueueCount(int blockingQueueCount) {
        this.blockingQueueCount = blockingQueueCount;
        return this;
    }

    @Override
    public BaseApplicationBuilder setHome(Path home) {
        this.home = home;
        return this;
    }

    @Override
    public BaseApplicationBuilder setContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    @Override
    public BaseApplicationBuilder setSecret(String secret) {
        this.secret = secret;
        return this;
    }

    @Override
    public BaseApplicationBuilder setSessionsEnabled(boolean sessionsEnabled) {
        this.sessionsEnabled = sessionsEnabled;
        return this;
    }

    @Override
    public BaseApplicationBuilder setRouter(HttpRouter router) {
        this.router = router;
        return this;
    }

    @Override
    public ApplicationBuilder setLocale(Locale locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public ApplicationBuilder setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    @Override
    public ApplicationBuilder addModule(ApplicationModule module) {
        if (module != null) {
            applicationModuleList.add(module);
            logger.log(Level.FINE, "module " + module + " added");
        }
        return this;
    }

    public ApplicationBuilder addStaticSuffixes(String... suffixes) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String suffix : suffixes) {
            builder.add(suffix);
        }
        this.staticFileSuffixes = builder.build(new String[]{});
        return this;
    }

    @Override
    public Application build() {
        Application application = new BaseApplication(this);
        setupApplication(application);
        return application;
    }

    protected void setupApplication(Application application) {
        ServiceLoader<ApplicationModule> serviceLoader = ServiceLoader.load(ApplicationModule.class);
        for (ApplicationModule module : serviceLoader) {
            applicationModuleList.add(module);
            logger.log(Level.FINE, "module " + module + " added");
        }
        applicationModuleList.forEach(module -> {
            try {
                module.onOpen(application);
                logger.log(Level.FINE, "module " + module + " opened");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        if (router != null) {
            router.setApplication(application);
        }
        if (staticFileSuffixes == null) {
            staticFileSuffixes = DEFAULT_SUFFIXES;
        }
    }

    private static final Set<String> DEFAULT_SUFFIXES = ImmutableSet.<String>builder()
            .add("css")
            .add("js")
            .add("ico")
            .add("png")
            .add("jpg")
            .add("jpeg")
            .add("gif")
            .add("woff2")
            .build(new String[]{});
}
