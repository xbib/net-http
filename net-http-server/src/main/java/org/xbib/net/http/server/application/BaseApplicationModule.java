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
    public void onSuccess(HttpRouterContext httpRouterContext) {
    }

    @Override
    public void onFail(HttpRouterContext httpRouterContext, Throwable throwable) {
    }

    @Override
    public void onOpen(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest) {
    }

    @Override
    public void onSuccess(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest) {
    }

    @Override
    public void onFail(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest, Throwable throwable) {
    }

    @Override
    public void onOpen(Session session) {
    }

    @Override
    public void onSuccess(Session session) {
    }

    @Override
    public void onFail(Session session, Throwable throwable) {
    }

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFail(Throwable throwable) {
    }
}
