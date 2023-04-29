package org.xbib.net.http.server.application.web;

import java.nio.file.Paths;
import java.time.Duration;

import org.xbib.net.http.cookie.SameSite;
import org.xbib.net.http.server.application.BaseApplication;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.session.IncomingSessionHandler;
import org.xbib.net.http.server.session.OutgoingSessionHandler;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.http.server.session.file.FileJsonSessionCodec;
import org.xbib.net.util.RandomUtil;

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

    protected Codec<Session> newSessionCodec(HttpRouterContext httpRouterContext) {
        return new FileJsonSessionCodec(sessionName, this, 1024, Duration.ofDays(1),
                Paths.get("/var/tmp/session"));
    }

    protected HttpHandler newIncomingSessionHandler(HttpRouterContext httpRouterContext) {
        @SuppressWarnings("unchecked")
        Codec<Session> sessionCodec = httpRouterContext.getAttributes().get(Codec.class, "sessioncodec");
        return new IncomingSessionHandler(
                getSecret(),
                "HmacSHA1",
                sessionName,
                sessionCodec,
                getStaticFileSuffixes(),
                "user_id",
                "e_user_id",
                () -> RandomUtil.randomString(16));
    }

    protected OutgoingSessionHandler newOutgoingSessionHandler(HttpRouterContext httpRouterContext) {
        @SuppressWarnings("unchecked")
        Codec<Session> sessionCodec = httpRouterContext.getAttributes().get(Codec.class, "sessioncodec");
        return new OutgoingSessionHandler(
                getSecret(),
                "HmacSHA1",
                sessionName,
                sessionCodec,
                getStaticFileSuffixes(),
                "user_id",
                "e_user_id",
                Duration.ofDays(1),
                true,
                false,
                SameSite.LAX);
    }
}
