package org.xbib.net.http.server.application;

import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.net.http.server.session.Session;

public interface ApplicationModule {

    String getName();

    void onOpen(HttpRouterContext httpRouterContext);

    void onOpen(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest);

    void onClose(HttpRouterContext httpRouterContext);

    void onOpen(Session session);

    void onClose(Session session);

    void onClose();
}
