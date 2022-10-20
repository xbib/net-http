package org.xbib.net.http.server.auth;

import org.xbib.net.SecurityRealm;
import org.xbib.net.UserProfile;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BasicAuthenticationHandler extends LoginAuthenticationHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(BasicAuthenticationHandler.class.getName());

    public BasicAuthenticationHandler(SecurityRealm securityRealm) {
        super(null, null, securityRealm);
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        HttpRequest httpRequest = context.httpRequest();
        UserProfile userProfile = context.attributes().get(UserProfile.class, "userprofile");
        if (userProfile != null && userProfile.getUserId() != null) {
            return;
        }
        String authorization = httpRequest.getHeaders().get(HttpHeaderNames.AUTHORIZATION);
        if (authorization != null) {
            if (!authorization.startsWith("Basic ")) {
                return;
            }
            byte[] b = Base64.getDecoder().decode(authorization.substring("Basic ".length()));
            String[] tokens = new String(b).split(":");
            if (tokens.length != 2) {
                return;
            }
            userProfile = new BaseUserProfile();
            try {
                authenticate(userProfile, tokens[0], tokens[1], httpRequest);
                context.attributes().put("userprofile", userProfile);
                return;
            } catch (Exception e) {
                logger.log(Level.WARNING, "authentication error");
            }
        } else {
            logger.log(Level.WARNING, "no authorization header");
        }
        logger.log(Level.INFO, "unauthenticated");
        context.response().setResponseStatus(HttpResponseStatus.UNAUTHORIZED)
                        .setHeader("WWW-Authenticate", "Basic realm=\"" + securityRealm.getName() + "\"");
    }
}
