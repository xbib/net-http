package org.xbib.net.http.server.application;

public interface Resolver<R> {

    R resolve(String string);
}
