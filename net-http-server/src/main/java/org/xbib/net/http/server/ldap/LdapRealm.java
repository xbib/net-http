package org.xbib.net.http.server.ldap;

import java.util.Map;
import org.xbib.net.Authenticator;
import org.xbib.net.GroupsProvider;
import org.xbib.net.UsersProvider;
import org.xbib.net.SecurityRealm;

public class LdapRealm extends SecurityRealm {

    private final String name;

    private final Map<String, LdapContextFactory> contextFactories;

    private final LdapUsersProvider usersProvider;

    private final LdapGroupsProvider groupsProvider;

    private final LdapAuthenticator authenticator;

    public LdapRealm(String name,
                     Map<String, LdapContextFactory> contextFactories,
                     Map<String, LdapUserMapping> userMappings,
                     Map<String, LdapGroupMapping> groupMappings) {
        this.name = name;
        this.contextFactories = contextFactories;
        this.usersProvider = new LdapUsersProvider(contextFactories, userMappings);
        this.groupsProvider = new LdapGroupsProvider(contextFactories, userMappings, groupMappings);
        this.authenticator = new LdapAuthenticator(contextFactories, userMappings);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void init() {
        for (LdapContextFactory contextFactory : contextFactories.values()) {
            contextFactory.testConnection();
        }
    }

    @Override
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    @Override
    public UsersProvider getUsersProvider() {
        return usersProvider;
    }

    @Override
    public GroupsProvider getGroupsProvider() {
        return groupsProvider;
    }

}
