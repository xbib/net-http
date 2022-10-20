package org.xbib.net.http.client;

@FunctionalInterface
public interface ExceptionListener {

    void onException(Throwable throwable);
}
