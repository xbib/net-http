package org.xbib.net.http.server.nio;

import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.HttpServerConfig;

public class NioHttpServerBuilder {

    HttpServerConfig httpServerConfig;

    Application application;

    NioHttpServerBuilder() {
    }

    public NioHttpServerBuilder setHttpServerConfig(HttpServerConfig httpServerConfig) {
        this.httpServerConfig = httpServerConfig;
        return this;
    }

    public NioHttpServerBuilder setApplication(Application application) {
        this.application = application;
        return this;
    }

    public NioHttpServer build() {
        return new NioHttpServer(this);
    }
}
