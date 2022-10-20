package org.xbib.net.http.server;

public interface Resolver<R> {

    R resolve(String string);
}
