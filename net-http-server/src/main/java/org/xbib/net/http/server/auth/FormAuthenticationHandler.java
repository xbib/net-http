package org.xbib.net.http.server.auth;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.Parameter;
import org.xbib.net.SecurityRealm;
import org.xbib.net.URL;
import org.xbib.net.UserProfile;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpServerContext;

public class FormAuthenticationHandler extends LoginAuthenticationHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(FormAuthenticationHandler.class.getName());

    String usernameParameter;

    String passwordParameter;

    String rememberParameter;

    String loginPage;

    public FormAuthenticationHandler(String usernameParameter,
                                     String passwordParameter,
                                     String rememberParameter,
                                     String loginPage,
                                     SecurityRealm securityRealm) {
        super(usernameParameter, passwordParameter, securityRealm);
        this.usernameParameter = usernameParameter;
        this.passwordParameter = passwordParameter;
        this.rememberParameter = rememberParameter;
        this.loginPage = loginPage;
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        if (loginPage == null) {
            logger.log(Level.WARNING, "no loginPage configured");
            return;
        }
        UserProfile userProfile = context.getAttributes().get(UserProfile.class, "userprofile");
        if (userProfile != null && userProfile.getUserId() != null) {
            logger.log(Level.FINE, "user id already set: " + userProfile.getUserId());
            return;
        }
        // always add an "anonymous" user profile
        userProfile = new BaseUserProfile();
        context.getAttributes().put("userprofile", userProfile);
        Parameter parameter = context.httpRequest().getParameter();
        if (!parameter.containsKey(usernameParameter, Parameter.Domain.FORM)) {
            logger.log(Level.WARNING, "usernameParameter not set, unable to authenticate");
            prepareFormAuthentication(context);
            return;
        }
        if (!parameter.containsKey(passwordParameter, Parameter.Domain.FORM)) {
            logger.log(Level.WARNING, "passwordParameter not set, unable to authenticate");
            prepareFormAuthentication(context);
            return;
        }
        String username = parameter.getAsString(usernameParameter, Parameter.Domain.FORM);
        String password = parameter.getAsString(passwordParameter, Parameter.Domain.FORM);
        logger.log(Level.FINE, "username and password found, ready for authentication");
        try {
            authenticate(userProfile, username, password, context.httpRequest());
            logger.log(Level.FINE, "successful authentication");
            return;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "authentication error for " + username);
        }
        prepareFormAuthentication(context);
    }

    private void prepareFormAuthentication(HttpServerContext context) {
        // this will redirect internally to login page, and back to the original path.
        // We need a full path resolve against the server URL.
        logger.log(Level.FINE, "templatePath = " + loginPage);
        context.getAttributes().put("templatePath", loginPage);
        URL loc = context.getContextURL().resolve(context.httpRequest().getRequestURI()).normalize();
        logger.log(Level.FINE, "context URL = " + context.getContextURL() + " request URI = " + context.httpRequest().getRequestURI() + " loc = " + loc);
        context.getAttributes().put("originalPath", loc.toExternalForm());
    }
}
