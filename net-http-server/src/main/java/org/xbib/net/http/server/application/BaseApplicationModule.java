package org.xbib.net.http.server.application;

import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;
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
    public void onOpen(Application application, Settings settings) throws Exception {
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext) {
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService) {
    }

    @Override
    public void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest) {
    }

    @Override
    public void onClose(Application application, HttpServerContext httpServerContext) {
    }

    @Override
    public void onOpen(Application application, Session session) {
    }

    @Override
    public void onClose(Application application, Session session) {
    }

    @Override
    public void onClose(Application application) {
    }
}
