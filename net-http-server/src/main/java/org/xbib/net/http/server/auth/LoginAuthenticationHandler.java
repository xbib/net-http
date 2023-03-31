package org.xbib.net.http.server.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.Authenticator;
import org.xbib.net.GroupsProvider;
import org.xbib.net.Parameter;
import org.xbib.net.Request;
import org.xbib.net.SecurityRealm;
import org.xbib.net.UserDetails;
import org.xbib.net.UserProfile;
import org.xbib.net.UsersProvider;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpServerContext;

public class LoginAuthenticationHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(LoginAuthenticationHandler.class.getName());

    private final String userParameterName;

    private final String passwordParameterName;

    protected final SecurityRealm securityRealm;

    public LoginAuthenticationHandler(String userParameterName,
                                      String passwordParameterName,
                                      SecurityRealm securityRealm) {
        this.userParameterName = userParameterName;
        this.passwordParameterName = passwordParameterName;
        this.securityRealm = securityRealm;
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        UserProfile userProfile = context.getAttributes().get(UserProfile.class, "userprofile");
        if (userProfile != null && userProfile.getUserId() != null) {
            return;
        }
        userProfile = new BaseUserProfile();
        try {
            authenticate(userProfile,
                    (String) context.httpRequest().getParameter().get(userParameterName, Parameter.Domain.FORM),
                    (String) context.httpRequest().getParameter().get(passwordParameterName, Parameter.Domain.FORM),
                    context.httpRequest());
            context.getAttributes().put("userprofile", userProfile);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "authentication error");
        }
    }

    protected void authenticate(UserProfile userProfile, String username, String password, Request request) {
        if (username == null) {
            logger.log(Level.FINE, "no username given for check, doing nothing");
            return;
        }
        if (password == null) {
            logger.log(Level.FINE, "no password given for check, doing nothing");
            return;
        }
        Authenticator auth = securityRealm.getAuthenticator();
        Authenticator.Context authContext = new Authenticator.Context(username, password, request);
        if (auth.authenticate(authContext)) {
            userProfile.setUserId(authContext.getUsername());
        }
        UsersProvider.Context userContext = new UsersProvider.Context(username, null);
        UserDetails userDetails = securityRealm.getUsersProvider().getUserDetails(userContext);
        userProfile.setEffectiveUserId(userDetails.getEffectiveUserId());
        userProfile.setName(userDetails.getName());
        GroupsProvider.Context groupContext = new GroupsProvider.Context(username, null);
        Collection<String> groups = securityRealm.getGroupsProvider().getGroups(groupContext);
        for (String group : groups) {
            userProfile.addRole(group);
        }
        logger.log(Level.FINE, "authenticate: userProfile = " + userProfile);
    }
}
