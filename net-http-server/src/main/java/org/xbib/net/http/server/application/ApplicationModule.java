package org.xbib.net.http.server.application;

import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.net.http.server.session.Session;

public interface ApplicationModule {

    String getName();

    void onOpen(HttpServerContext httpServerContext);

    void onOpen(HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest);

    void onClose(HttpServerContext httpServerContext);

    void onOpen(Session session);

    void onClose(Session session);

    void onClose();
}
