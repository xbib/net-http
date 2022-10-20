package org.xbib.net.http.server.simple.secure;

import org.xbib.net.http.server.simple.SimpleHttpServer;
import org.xbib.net.http.server.simple.SimpleHttpServerBuilder;

public class SimpleHttpsServerBuilder extends SimpleHttpServerBuilder {

    protected SimpleHttpsServerBuilder() {
    }

    @Override
    public SimpleHttpsServer build() {
        return new SimpleHttpsServer(this);
    }
}
