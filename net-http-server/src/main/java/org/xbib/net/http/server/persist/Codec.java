package org.xbib.net.http.server.persist;

import java.io.IOException;

public interface Codec<D> {

    D create(String key) throws IOException;

    D read(String key) throws IOException;

    void write(String key, D data) throws IOException;

    void remove(String key) throws IOException;

    void purge(long expiredAfterSeconds) throws IOException;
}
