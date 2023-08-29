package org.xbib.net.http.server.application;

import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.net.http.server.session.Session;

public interface ApplicationModule {

    String getName();

    void onOpen(HttpRouterContext httpRouterContext);

    void onSuccess(HttpRouterContext httpRouterContext);

    void onFail(HttpRouterContext httpRouterContext, Throwable throwable);

    void onOpen(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest);

    void onSuccess(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest);

    void onFail(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest, Throwable throwable);

    void onOpen(Session session);

    void onSuccess(Session session);

    void onFail(Session session, Throwable throwable);

    void onSuccess();

    void onFail(Throwable throwable);
}
