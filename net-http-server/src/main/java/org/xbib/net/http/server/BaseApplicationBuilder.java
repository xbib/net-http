package org.xbib.net.http.server;

import org.xbib.config.ConfigLoader;
import org.xbib.config.ConfigLogger;
import org.xbib.config.ConfigParams;
import org.xbib.config.SystemConfigLogger;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.settings.Settings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseApplicationBuilder implements ApplicationBuilder {

    private static final Logger logger = Logger.getLogger(BaseApplicationBuilder.class.getName());

    private static final ConfigLogger bootLogger;

    static {
        // early loading of boot logger during static initialization block
        ServiceLoader<ConfigLogger> serviceLoader = ServiceLoader.load(ConfigLogger.class);
        Optional<ConfigLogger> optionalBootLogger = serviceLoader.findFirst();
        bootLogger = optionalBootLogger.orElse(new SystemConfigLogger());
    }

    protected ClassLoader classLoader;

    protected int blockingThreadCount;

    protected int blockingQueueCount;

    protected Path home;

    protected String contextPath;

    protected String secret;

    protected boolean sessionsEnabled;

    protected HttpRouter router;

    protected Locale locale;

    protected ZoneId zoneId;

    protected Set<String> staticFileSuffixes;

    protected ConfigParams configParams;

    protected ConfigLoader configLoader;

    protected Settings settings;

    protected List<ApplicationModule> applicationModuleList;

    protected BaseApplicationBuilder() {
        this.classLoader = getClass().getClassLoader();
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

    public BaseApplicationBuilder setSettings(Settings settings) {
        this.settings = settings;
        return this;
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
    public ApplicationBuilder setRouter(HttpRouter router) {
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

    public ApplicationBuilder setStaticSuffixes(String... suffixes) {
        this.staticFileSuffixes = Set.of(suffixes);
        return this;
    }

    public ApplicationBuilder registerModule(ApplicationModule applicationModule) {
        applicationModuleList.add(applicationModule);
        return this;
    }

    @Override
    public Application build() {
        prepareApplication();
        Application application = new BaseApplication(this);
        setupApplication(application);
        return application;
    }

    protected void prepareApplication() {
        String name = System.getProperty("application.name");
        if (name == null) {
            name = "application";
        }
        String profile = System.getProperty("application.profile");
        if (profile == null) {
            profile = "developer";
        }
        String[] args = profile.split(";");
        this.configParams = new ConfigParams()
                .withArgs(args)
                .withDirectoryName(name)
                .withFileNamesWithoutSuffix(args[0])
                .withSystemEnvironment()
                .withSystemProperties();
        this.configLoader = ConfigLoader.getInstance()
                .withLogger(bootLogger);
        this.settings = configLoader.load(configParams);
        if (staticFileSuffixes == null) {
            staticFileSuffixes = DEFAULT_SUFFIXES;
        }
    }

    protected void setupApplication(Application application) {
        if (router != null) {
            router.setApplication(application);
        }
        for (Map.Entry<String, Settings> entry : settings.getGroups("module").entrySet()) {
            String moduleName = entry.getKey();
            Settings moduleSettings = entry.getValue();
            if (moduleSettings.getAsBoolean("enabled", true)) {
                try {
                    String className = moduleSettings.get("class");
                    @SuppressWarnings("unchecked")
                    Class<ApplicationModule> clazz =
                        (Class<ApplicationModule>) Class.forName(className, true, classLoader);
                    ApplicationModule applicationModule = clazz.getConstructor(Application.class, String.class, Settings.class)
                            .newInstance(application, moduleName, moduleSettings);
                    applicationModuleList.add(applicationModule);
                    applicationModule.onOpen(application, moduleSettings);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                    throw new IllegalArgumentException("class not found or not loadable: " + e.getMessage());
                }
            } else {
                logger.log(Level.WARNING, "disabled module: " + moduleName);
            }
        }
    }

    private static final Set<String> DEFAULT_SUFFIXES =
            Set.of("css", "js", "ico", "png", "jpg", "jpeg", "gif", "woff2");
}
