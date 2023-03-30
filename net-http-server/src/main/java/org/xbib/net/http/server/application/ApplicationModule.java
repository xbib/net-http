package org.xbib.net.http.server.application;

import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.net.http.server.session.Session;
import org.xbib.settings.Settings;

public interface ApplicationModule {

    String getName();

    void onOpen(Application application, Settings settings) throws Exception;

    void onOpen(Application application, HttpServerContext httpServerContext);

    void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService);

    void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest);

    void onClose(Application application, HttpServerContext httpServerContext);

    void onOpen(Application application, Session session);

    void onClose(Application application, Session session);

    void onClose(Application application);
}
