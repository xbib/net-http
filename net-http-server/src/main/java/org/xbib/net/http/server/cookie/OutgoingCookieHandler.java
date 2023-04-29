package org.xbib.net.http.server.cookie;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.cookie.CookieBox;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

public class OutgoingCookieHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(OutgoingCookieHandler.class.getName());

    public OutgoingCookieHandler() {
    }

    @Override
    public void handle(HttpRouterContext context) throws HttpException {
        CookieBox cookieBox = context.getAttributes().get(CookieBox.class, "outgoingcookies");
        if (cookieBox != null) {
            for (Cookie cookie : cookieBox) {
                context.cookie(cookie);
                logger.log(Level.FINEST, "cookie prepared for outgoing = " + cookie);
            }
        }
    }
}
