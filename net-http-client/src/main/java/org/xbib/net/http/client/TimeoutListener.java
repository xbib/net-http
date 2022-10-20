package org.xbib.net.http.client;

@FunctionalInterface
public interface TimeoutListener {

    void onTimeout(HttpRequest httpRequest);
}
