package org.xbib.net.http.server.session;

public interface SessionListener {

    void onCreated(Session session);

    void onDestroy(Session session);
}
