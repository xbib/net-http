package org.xbib.net.http.server.application;

import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.net.http.server.session.Session;
import org.xbib.settings.Settings;

public abstract class BaseApplicationModule implements ApplicationModule {

    protected final Application application;

    protected final String name;

    protected final Settings settings;

    public BaseApplicationModule(Application application, String name, Settings settings) {
        this.application = application;
        this.name = name;
        this.settings = settings;
    }

    @Override
    public String getName() {
        return name != null ? name : settings.get("name", "undefined");
    }

    @Override
    public void onOpen(HttpRouterContext httpRouterContext) {
    }

    @Override
    public void onOpen(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest) {
    }

    @Override
    public void onClose(HttpRouterContext httpRouterContext) {
    }

    @Override
    public void onOpen(Session session) {
    }

    @Override
    public void onClose(Session session) {
    }

    @Override
    public void onClose() {
    }
}
