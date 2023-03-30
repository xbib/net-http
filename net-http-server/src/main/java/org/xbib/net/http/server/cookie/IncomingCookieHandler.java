package org.xbib.net.http.server.cookie;

import java.util.Collection;
import java.util.logging.Logger;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.cookie.CookieBox;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpServerContext;

public class IncomingCookieHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(IncomingCookieHandler.class.getName());

    public IncomingCookieHandler() {
    }

    @Override
    public void handle(HttpServerContext context) throws HttpException {
        Collection<String> cookieStrings = context.request().getHeaders().getAll(HttpHeaderNames.COOKIE);
        if (cookieStrings.isEmpty()) {
            return;
        }
        CookieBox cookieBox = new CookieBox();
        for (String cookieString : cookieStrings) {
            if (cookieString == null || cookieString.isEmpty()) {
                continue;
            }
            cookieBox.addAll(CookieDecoder.LAX.decode(cookieString));
        }
        if (!cookieBox.isEmpty()) {
            context.getAttributes().put("incomingcookies", cookieBox);

        }
    }
}
