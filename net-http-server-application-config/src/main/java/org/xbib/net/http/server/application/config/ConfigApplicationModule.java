package org.xbib.net.http.server.application.config;

import java.util.Optional;
import java.util.ServiceLoader;
import org.xbib.config.ConfigLoader;
import org.xbib.config.ConfigLogger;
import org.xbib.config.ConfigParams;
import org.xbib.config.SystemConfigLogger;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.application.BaseApplicationModule;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;
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

    private ConfigParams configParams;

    private ConfigLoader configLoader;

    private Settings settings;

    public ConfigApplicationModule(Application application, String name, Settings settings) {
        super(application, name, settings);
    }

    @Override
    public void onOpen(Application application, Settings settings) {
        String profile = System.getProperty("application.profile");
        if (profile == null) {
            profile = "developer";
        }
        String[] args = profile.split(";");
        this.configParams = new ConfigParams()
                .withArgs(args)
                .withDirectoryName("application")
                .withFileNamesWithoutSuffix(args[0])
                .withSystemEnvironment()
                .withSystemProperties();
        this.configLoader = ConfigLoader.getInstance()
                .withLogger(bootLogger);
        this.settings = configLoader.load(configParams);
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService) {
        httpServerContext.getAttributes().put("configparams", configParams);
        httpServerContext.getAttributes().put("configloader", configLoader);
        httpServerContext.getAttributes().put("settings", settings);
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest) {
        // do nothing
    }
}
