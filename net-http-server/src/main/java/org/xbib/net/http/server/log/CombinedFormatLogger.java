package org.xbib.net.http.server.log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.UserProfile;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;

public class CombinedFormatLogger implements HttpHandler {

    private static final Logger accessLogger = Logger.getLogger(CommonFormatLogger.class.getName());

    private static final String LOG_FORMAT =
            "%1$s - %10$s - [%2$td/%2$tb/%2$tY:%2$tT %2$tz] \"%3$s %4$s %5$s\" %6$d %7$d \"%8$s\" \"%9$s\"";

    @Override
    public void handle(HttpServerContext httpServerContext) throws IOException {
        HttpRequest request = httpServerContext.httpRequest();
        InetSocketAddress remote = httpServerContext.httpRequest().getRemoteAddress();
        String inetAddressString = remote.getHostName() + ":" + remote.getPort();
        HttpResponseStatus httpResponseStatus = httpServerContext.response().getResponseStatus();
        int statusInteger = httpResponseStatus != null ? httpResponseStatus.code() : 0;
        Long contentLength = httpServerContext.response().getLength();
        UserProfile userProfile = httpServerContext.attributes().get(UserProfile.class, "userprofile");
        String user = userProfile != null ? userProfile.getEffectiveUserId() : "";
        String referer = request.getHeaders().get(HttpHeaderNames.REFERER);
        referer = referer != null ? referer : "";
        String useragent = request.getHeaders().get(HttpHeaderNames.USER_AGENT);
        useragent = useragent != null ? useragent : "";
        String message = String.format(Locale.US, LOG_FORMAT,
                inetAddressString,
                ZonedDateTime.now(),
                request.getMethod(),
                request.getRequestURI(),
                request.getContext().getContextURL().getScheme(),
                statusInteger,
                contentLength,
                referer,
                useragent,
                user
        );
        accessLogger.log(Level.INFO, message);
    }
}
