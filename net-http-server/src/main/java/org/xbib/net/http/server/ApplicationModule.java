package org.xbib.net.http.server;

import org.xbib.net.http.server.session.Session;

public interface ApplicationModule extends Comparable<ApplicationModule> {

    String getName();

    void onOpen(Application application) throws Exception;

    void onOpen(Application application, HttpServerContext httpServerContext);

    void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService);

    void onOpen(Application application, HttpServerContext httpServerContext, HttpService httpService, HttpRequest httpRequest);

    void onClose(Application application, HttpServerContext httpServerContext);

    void onOpen(Application application, Session session);

    void onClose(Application application, Session session);

    void onClose(Application application);
}
