package org.xbib.net.http.server.ldap;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.xbib.net.Authenticator;

public class LdapAuthenticator extends Authenticator {

    private static final Logger logger = Logger.getLogger(LdapAuthenticator.class.getName());

    private final Map<String, LdapContextFactory> contextFactories;

    private final Map<String, LdapUserMapping> userMappings;

    public LdapAuthenticator(Map<String, LdapContextFactory> contextFactories,
                             Map<String, LdapUserMapping> userMappings) {
        this.contextFactories = contextFactories;
        this.userMappings = userMappings;
    }

    @Override
    public boolean authenticate(Context context) {
        return authenticate(context.getUsername(), context.getPassword());
    }

    /**
     * Authenticate the user against LDAP servers until first success.
     * @param login The login to use.
     * @param password The password to use.
     * @return false if specified user cannot be authenticated with specified password on any LDAP server
     */
    public boolean authenticate(String login, String password) {
        for (String ldapKey : userMappings.keySet()) {
            final String principal;
            if (contextFactories.get(ldapKey).isSasl()) {
                principal = login;
            } else {
                final SearchResult result;
                try {
                    result = userMappings.get(ldapKey).createSearch(contextFactories.get(ldapKey), login).findUnique();
                } catch (NamingException e) {
                    logger.log(Level.FINE, "user " + login + " not found in server " + ldapKey + ": " + e.getMessage());
                    continue;
                }
                if (result == null) {
                    logger.log(Level.FINE, "user " + login + " not found in " + ldapKey);
                    continue;
                }
                principal = result.getNameInNamespace();
            }
            boolean passwordValid;
            if (contextFactories.get(ldapKey).isGssapi()) {
                passwordValid = checkPasswordUsingGssapi(principal, password, ldapKey);
            } else {
                passwordValid = checkPasswordUsingBind(principal, password, ldapKey);
            }
            if (passwordValid) {
                return true;
            }
        }
        logger.log(Level.FINE, "user not found: " + login);
        return false;
    }

    private boolean checkPasswordUsingBind(String principal, String password, String ldapKey) {
        if (password.isEmpty()) {
            logger.log(Level.FINE, "password is blank");
            return false;
        }
        InitialDirContext context = null;
        try {
            context = contextFactories.get(ldapKey).createUserContext(principal, password);
            return true;
        } catch (NamingException e) {
            logger.log(Level.FINE, "password not valid for user " + principal + " in server " + ldapKey + ": " + e.getMessage());
            return false;
        } finally {
            closeQuietly(context);
        }
    }

    private boolean checkPasswordUsingGssapi(String principal, String password, String ldapKey) {
        Configuration.setConfiguration(new Krb5LoginConfiguration());
        LoginContext lc;
        try {
            lc = new LoginContext(getClass().getName(), new CallbackHandlerImpl(principal, password));
            lc.login();
        } catch (LoginException e) {
            logger.log(Level.FINE, "password not valid for " + principal + " in server " + ldapKey + ": " + e.getMessage());
            return false;
        }
        try {
            lc.logout();
        } catch (LoginException e) {
            logger.log(Level.WARNING, "logout fails", e);
        }
        return true;
    }

    private static void closeQuietly(javax.naming.Context context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (NamingException e) {
            logger.log(Level.WARNING, "NamingException thrown while closing context", e);
        }
    }
}
