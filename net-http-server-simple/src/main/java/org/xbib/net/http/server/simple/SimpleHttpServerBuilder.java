package org.xbib.net.http.server.simple;

import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.HttpServerConfig;

public class SimpleHttpServerBuilder {

    HttpServerConfig httpServerConfig;

    Application application;

    protected SimpleHttpServerBuilder() {
    }

    public SimpleHttpServerBuilder setHttpServerConfig(HttpServerConfig httpServerConfig) {
        this.httpServerConfig = httpServerConfig;
        return this;
    }

    public SimpleHttpServerBuilder setApplication(Application application) {
        this.application = application;
        return this;
    }

    public SimpleHttpServer build() {
        return new SimpleHttpServer(this);
    }
}
