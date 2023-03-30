package org.xbib.net.http.server.ldap;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.xbib.net.UserDetails;
import org.xbib.net.UsersProvider;
import static java.lang.String.format;

public class LdapUsersProvider extends UsersProvider {

    private static final Logger logger = Logger.getLogger(LdapUsersProvider.class.getName());

    private final Map<String, LdapContextFactory> contextFactories;

    private final Map<String, LdapUserMapping> userMappings;

    public LdapUsersProvider(Map<String, LdapContextFactory> contextFactories,
                             Map<String, LdapUserMapping> userMappings) {
        this.contextFactories = contextFactories;
        this.userMappings = userMappings;
    }

    @Override
    public UserDetails getUserDetails(Context context) {
        return getUserDetails(context.getUsername());
    }

    /**
     * Get user details.
     *
     * @return details for specified user, or null if such user doesn't exist
     * @throws LdapException if unable to retrieve details
     */
    public UserDetails getUserDetails(String username) {
        logger.log(Level.FINE, "requesting details for user " + username);
        if (userMappings.isEmpty()) {
            String errorMessage = format("Unable to retrieve details for user %s: No user mapping found.", username);
            logger.log(Level.FINE, errorMessage);
            throw new LdapException(errorMessage);
        }
        UserDetails details = null;
        LdapException exception = null;
        for (String serverKey : userMappings.keySet()) {
            SearchResult searchResult = null;
            try {
                searchResult = userMappings.get(serverKey).createSearch(contextFactories.get(serverKey), username)
                        .returns(userMappings.get(serverKey).getRealNameAttribute(),
                                userMappings.get(serverKey).getUidAttribute())
                        .findUnique();
            } catch (NamingException e) {
                // just in case if Sonar silently swallowed exception
                logger.log(Level.FINE, e.getMessage(), e);
                exception = new LdapException("Unable to retrieve details for user " + username + " in " + serverKey, e);
            }
            if (searchResult != null) {
                try {
                    details = mapUserDetails(serverKey, searchResult);
                    // if no exceptions occur, we found the user and mapped his details.
                    break;
                } catch (NamingException e) {
                    // just in case if Sonar silently swallowed exception
                    logger.log(Level.FINE, e.getMessage(), e);
                    exception = new LdapException("Unable to retrieve details for user " + username + " in " + serverKey, e);
                }
            } else {
                // user not found
                logger.log(Level.FINE, "User " + username + " not found in " + serverKey);
            }
        }
        if (details == null && exception != null) {
            // No user found and there is an exception so there is a reason the user could not be found.
            throw exception;
        }
        return details;
    }

    /**
     * Map the properties from LDAP to the {@link UserDetails}.
     *
     * @param serverKey the LDAP index so we use the correct {@link LdapUserMapping}
     * @return If no exceptions are thrown, a {@link UserDetails} object containing the values from LDAP.
     * @throws NamingException In case the communication or mapping to the LDAP server fails.
     */
    private UserDetails mapUserDetails(String serverKey, SearchResult searchResult) throws NamingException {
        Attributes attributes = searchResult.getAttributes();
        UserDetails details = new UserDetails();
        details.setUserId(getAttributeValue(attributes.get(userMappings.get(serverKey).getUidAttribute())));
        details.setName(getAttributeValue(attributes.get(userMappings.get(serverKey).getRealNameAttribute())));
        return details;
    }

    private static String getAttributeValue(Attribute attribute) throws NamingException {
        if (attribute == null) {
            return "";
        }
        return (String) attribute.get();
    }
}
