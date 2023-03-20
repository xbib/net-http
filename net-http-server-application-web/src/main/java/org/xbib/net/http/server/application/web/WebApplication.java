package org.xbib.net.http.server.application.web;

import java.nio.file.Paths;
import java.time.Duration;
import org.xbib.net.http.server.BaseApplication;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.session.IncomingSessionHandler;
import org.xbib.net.http.server.session.OutgoingSessionHandler;
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

    protected Codec<Session> buildSessionCodec(HttpServerContext httpServerContext) {
        return new FileJsonSessionCodec(sessionName, this, 1024, Duration.ofDays(1),
                Paths.get("/var/tmp/session"));
    }

    protected HttpHandler buildIncomingSessionHandler(HttpServerContext httpServerContext) {
        @SuppressWarnings("unchecked")
        Codec<Session> sessionCodec = httpServerContext.attributes().get(Codec.class, "sessioncodec");
        return new IncomingSessionHandler(
                getSecret(),
                "HmacSHA1",
                sessionName,
                sessionCodec,
                getStaticFileSuffixes(),
                "user_id",
                "e_user_id");
    }

    protected OutgoingSessionHandler buildOutgoingSessionHandler(HttpServerContext httpServerContext) {
        @SuppressWarnings("unchecked")
        Codec<Session> sessionCodec = httpServerContext.attributes().get(Codec.class, "sessioncodec");
        return new OutgoingSessionHandler(
                getSecret(),
                "HmacSHA1",
                sessionName,
                Duration.ofDays(1),
                sessionCodec,
                getStaticFileSuffixes(),
                "user_id",
                "e_user_id");
    }
}
