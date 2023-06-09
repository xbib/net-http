package org.xbib.net.http.server.application.web;

import java.nio.file.Paths;
import java.time.Duration;

import org.xbib.net.http.server.application.BaseApplication;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.http.server.session.file.FileJsonSessionCodec;

public class WebApplication extends BaseApplication {

    protected WebApplication(WebApplicationBuilder builder) {
        super(builder);
    }

    public static WebApplicationBuilder builder() {
        try {
            return new WebApplicationBuilder();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Codec<Session> newSessionCodec(HttpRouterContext httpRouterContext) {
        return new FileJsonSessionCodec(sessionName, this, 1024, Duration.ofDays(1),
                Paths.get("/var/tmp/session"));
    }
}
