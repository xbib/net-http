package org.xbib.net.http.server.application;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import org.xbib.config.ConfigLoader;
import org.xbib.config.ConfigLogger;
import org.xbib.config.ConfigParams;
import org.xbib.config.SystemConfigLogger;
import org.xbib.net.http.server.executor.BaseExecutor;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.mime.MimeTypeService;
import org.xbib.settings.Settings;

public class BaseApplicationBuilder implements ApplicationBuilder {

    private static final ConfigLogger bootLogger;

    static {
        // early loading of boot logger during static initialization block
        ServiceLoader<ConfigLogger> serviceLoader = ServiceLoader.load(ConfigLogger.class);
        Optional<ConfigLogger> optionalBootLogger = serviceLoader.findFirst();
        bootLogger = optionalBootLogger.orElse(new SystemConfigLogger());
    }

    private static final Set<String> DEFAULT_SUFFIXES =
            Set.of("css", "js", "ico", "png", "jpg", "jpeg", "gif", "woff2");

    protected ClassLoader classLoader;

    protected Path home;

    protected String contextPath;

    protected String secret;

    protected boolean sessionsEnabled;

    protected Locale locale;

    protected ZoneId zoneId;

    protected MimeTypeService mimeTypeService;

    protected Set<String> staticFileSuffixes;

    protected ConfigParams configParams;

    protected ConfigLoader configLoader;

    protected Settings settings;

    protected Executor executor;

    protected HttpRouter httpRouter;

    protected BaseApplicationBuilder() {
        this.classLoader = getClass().getClassLoader();
        this.home = Paths.get(System.getProperties().containsKey("application.home") ? System.getProperty("application.home") : ".");
        this.contextPath = "/";
        this.secret = "secret";
        this.sessionsEnabled = true;
        this.locale = Locale.getDefault();
        this.zoneId = ZoneId.systemDefault();
        this.mimeTypeService = new MimeTypeService();
        String name = System.getProperty("application.name");
        if (name == null) {
            name = "application";
        }
        String profile = System.getProperty("application.profile");
        if (profile == null) {
            profile = "developer";
        }
        this.configParams = new ConfigParams()
                .withDirectoryName(name)
                .withFileNamesWithoutSuffix(profile)
                .withSystemEnvironment()
                .withSystemProperties();
        this.configLoader = ConfigLoader.getInstance()
                .withLogger(bootLogger);
        this.settings = configLoader.load(configParams);
        if (staticFileSuffixes == null) {
            staticFileSuffixes = DEFAULT_SUFFIXES;
        }
        this.executor = BaseExecutor.builder().build();
    }

    public BaseApplicationBuilder setSettings(Settings settings) {
        this.settings = settings;
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
    public ApplicationBuilder setSecret(String secret) {
        this.secret = secret;
        return this;
    }

    @Override
    public ApplicationBuilder setSessionsEnabled(boolean sessionsEnabled) {
        this.sessionsEnabled = sessionsEnabled;
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
    public ApplicationBuilder setMimeTypeService(MimeTypeService mimeTypeService) {
        this.mimeTypeService = mimeTypeService;
        return this;
    }

    @Override
    public ApplicationBuilder setStaticSuffixes(String... suffixes) {
        this.staticFileSuffixes = Set.of(suffixes);
        return this;
    }

    @Override
    public ApplicationBuilder setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public ApplicationBuilder setRouter(HttpRouter httpRouter) {
        this.httpRouter = httpRouter;
        return this;
    }

    @Override
    public Application build() {
        Objects.requireNonNull(httpRouter, "http router must not be null");
        return new BaseApplication(this);
    }
}
