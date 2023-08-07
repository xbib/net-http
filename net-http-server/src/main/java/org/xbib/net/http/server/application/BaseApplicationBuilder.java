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

    private String name;

    private String profile;

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
        this.sessionsEnabled = true;
    }

    public BaseApplicationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public BaseApplicationBuilder setProfile(String profile) {
        this.profile = profile;
        return this;
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
        if (this.classLoader == null) {
            this.classLoader = getClass().getClassLoader();
        }
        if (this.home == null) {
            this.home = Paths.get(System.getProperty("application.home", "."));
        }
        if (this.contextPath == null) {
            this.contextPath = "/";
        }
        if (this.secret == null) {
            this.secret = "secret";
        }
        if (this.locale == null) {
            this.locale = Locale.getDefault();
        }
        if (this.zoneId == null) {
            this.zoneId = ZoneId.systemDefault();
        }
        if (name == null) {
            name = System.getProperty("application.name", "application");
        }
        if (profile == null) {
            profile = System.getProperty("application.profile", "developer");
        }
        if (this.settings == null) {
            this.configParams = new ConfigParams()
                    .withDirectoryName(name)
                    .withFileNamesWithoutSuffix(profile)
                    .withSystemEnvironment()
                    .withSystemProperties();
            this.configLoader = ConfigLoader.getInstance()
                    .withLogger(bootLogger);
            this.settings = configLoader.load(configParams);
        }
        if (this.mimeTypeService == null) {
            this.mimeTypeService = new MimeTypeService();
        }
        if (this.staticFileSuffixes == null) {
            this.staticFileSuffixes = DEFAULT_SUFFIXES;
        }
        if (executor == null) {
            this.executor = BaseExecutor.builder().build();
        }
        return new BaseApplication(this);
    }
}
