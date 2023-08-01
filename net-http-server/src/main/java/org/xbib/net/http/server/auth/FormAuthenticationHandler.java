package org.xbib.net.http.server.auth;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterException;
import org.xbib.net.SecurityRealm;
import org.xbib.net.URL;
import org.xbib.net.UserProfile;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

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
    public void handle(HttpRouterContext context) throws IOException {
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
        Parameter parameter = context.getRequest().getParameter();
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
        try {
            String username = parameter.getAsString(usernameParameter, Parameter.Domain.FORM);
            String password = parameter.getAsString(passwordParameter, Parameter.Domain.FORM);
            logger.log(Level.FINE, "username and password found, ready for authentication");
            authenticate(userProfile, username, password, context.getRequest());
            logger.log(Level.FINE, "successful authentication");
            return;
        } catch (ParameterException e) {
            logger.log(Level.SEVERE, "parameter error");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "authentication error");
        }
        prepareFormAuthentication(context);
    }

    private void prepareFormAuthentication(HttpRouterContext context) {
        // this will redirect internally to login page, and back to the original path.
        // We need a full path resolve against the server URL.
        logger.log(Level.FINE, "templatePath = " + loginPage);
        context.getAttributes().put("templatePath", loginPage);
        URL loc = context.getContextURL().resolve(context.getRequest().getRequestURI()).normalize();
        logger.log(Level.FINE, "context URL = " + context.getContextURL() + " request URI = " + context.getRequest().getRequestURI() + " loc = " + loc);
        context.getAttributes().put("originalPath", loc.toExternalForm());
    }
}
