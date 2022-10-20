package org.xbib.net.http.server.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.xbib.net.GroupsProvider;

public class LdapGroupsProvider extends GroupsProvider {

    private static final Logger logger = Logger.getLogger(LdapGroupsProvider.class.getName());

    private final Map<String, LdapContextFactory> contextFactories;

    private final Map<String, LdapUserMapping> userMappings;

    private final Map<String, LdapGroupMapping> groupMappings;

    public LdapGroupsProvider(Map<String, LdapContextFactory> contextFactories,
                              Map<String, LdapUserMapping> userMappings,
                              Map<String, LdapGroupMapping> groupMapping) {
        this.contextFactories = contextFactories;
        this.userMappings = userMappings;
        this.groupMappings = groupMapping;
    }

    @Override
    public Collection<String> getGroups(Context context) {
        return getGroups(context.getUsername());
    }

    /**
     * Get groups, or null if not possible.
     * @throws LdapException if unable to retrieve groups
     */
    public Collection<String> getGroups(String username) {
        if (userMappings == null) {
            return null;
        }
        if (groupMappings == null) {
            return null;
        }
        if (userMappings.isEmpty()) {
            throw new LdapException(String.format("Unable to retrieve details for user " + username + ": No user mapping found"));
        }
        if (groupMappings.isEmpty()) {
            throw new LdapException(String.format("Unable to retrieve details for user " + username + ": No group mapping found"));
        }
        Set<String> groups = new LinkedHashSet<>();
        List<LdapException> exceptions = new ArrayList<>();
        for (String serverKey : userMappings.keySet()) {
            if (!groupMappings.containsKey(serverKey)) {
                continue;
            }
            SearchResult searchResult = searchUserGroups(username, exceptions, serverKey);
            if (searchResult != null) {
                try {
                    NamingEnumeration<SearchResult> result = groupMappings.get(serverKey)
                            .createSearch(contextFactories.get(serverKey), searchResult).find();
                    groups.addAll(mapGroups(serverKey, result));
                    break;
                } catch (NamingException e) {
                    logger.log(Level.FINE, e.getMessage(), e);
                    exceptions.add(new LdapException(String.format("unable to retrieve groups for user %s in %s", username, serverKey), e));
                }
            }
        }
        checkResults(groups, exceptions);
        return groups;
    }

    private static void checkResults(Set<String> groups, List<LdapException> exceptions) {
        if (groups.isEmpty() && !exceptions.isEmpty()) {
            throw exceptions.iterator().next();
        }
    }

    private void checkPrerequisites(String username) {
    }

    private SearchResult searchUserGroups(String username, List<LdapException> exceptions, String serverKey) {
        SearchResult searchResult = null;
        try {
            logger.log(Level.INFO, "requesting groups for user " + username);
            searchResult = userMappings.get(serverKey).createSearch(contextFactories.get(serverKey), username)
                    .returns(groupMappings.get(serverKey).getFilterArgNames())
                    .findUnique();
        } catch (NamingException e) {
            logger.log(Level.FINE, e.getMessage(), e);
            exceptions.add(new LdapException(String.format("unable to retrieve groups for user %s in %s", username, serverKey), e));
        }
        return searchResult;
    }

    /**
     * Map all the groups.
     *
     * @param serverKey The index we use to choose the correct {@link LdapGroupMapping}.
     * @param searchResult The {@link SearchResult} from the search for the user.
     * @return A {@link Collection} of groups the user is member of.
     * @throws NamingException if name not found
     */
    private Collection<String> mapGroups(String serverKey, NamingEnumeration<SearchResult> searchResult) throws NamingException {
        Set<String> groups = new LinkedHashSet<>();
        while (searchResult.hasMoreElements()) {
            SearchResult obj = searchResult.nextElement();
            Attributes attributes = obj.getAttributes();
            String groupId = (String) attributes.get(groupMappings.get(serverKey).getIdAttribute()).get();
            groups.add(groupId);
        }
        return groups;
    }
}
