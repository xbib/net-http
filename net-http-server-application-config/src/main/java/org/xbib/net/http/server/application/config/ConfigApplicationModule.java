package org.xbib.net.http.server.application.config;

import java.util.Optional;
import java.util.ServiceLoader;
import org.xbib.config.ConfigLoader;
import org.xbib.config.ConfigLogger;
import org.xbib.config.ConfigParams;
import org.xbib.config.SystemConfigLogger;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.application.BaseApplicationModule;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.settings.Settings;

public class ConfigApplicationModule extends BaseApplicationModule {

    private static final ConfigLogger bootLogger;

    static {
        // early loading of boot logger during static initialization block
        ServiceLoader<ConfigLogger> serviceLoader = ServiceLoader.load(ConfigLogger.class);
        Optional<ConfigLogger> optionalBootLogger = serviceLoader.findFirst();
        bootLogger = optionalBootLogger.orElse(new SystemConfigLogger());
    }

    private final ConfigParams configParams;

    private final ConfigLoader configLoader;

    private final Settings settings;

    public ConfigApplicationModule(Application application, String name, Settings settings) {
        super(application, name, settings);
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
    }

    @Override
    public void onOpen(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest) {
        httpRouterContext.getAttributes().put("configparams", configParams);
        httpRouterContext.getAttributes().put("configloader", configLoader);
        httpRouterContext.getAttributes().put("settings", settings);
    }
}
