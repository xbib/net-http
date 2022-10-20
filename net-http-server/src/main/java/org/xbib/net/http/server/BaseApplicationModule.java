package org.xbib.net.http.server;

import org.xbib.net.http.server.session.Session;

public abstract class BaseApplicationModule implements ApplicationModule {

    public BaseApplicationModule() {
    }

    @Override
    public void onOpen(Application application) throws Exception {
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

    @Override
    public int compareTo(ApplicationModule o) {
        return getName().compareTo(o.getName());
    }
}
