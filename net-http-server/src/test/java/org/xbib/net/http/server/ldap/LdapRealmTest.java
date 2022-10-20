package org.xbib.net.http.server.ldap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.Authenticator;
import org.xbib.net.GroupsProvider;
import org.xbib.net.UsersProvider;
import org.xbib.net.UserDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LdapRealmTest {


    @Disabled
    @Test
    public void testRealm() {
        Map<String, LdapContextFactory> contextFactories = new HashMap<>();
        LdapContextFactory contextFactory = new LdapContextFactory("simple",
                "com.sun.jndi.ldap.LdapCtxFactory",
                null,
                "ldap://localhost:389",
                false,
                null,
                null,
                "follow"
        );
        contextFactories.put("default", contextFactory);
        Map<String, LdapUserMapping> userMappings = new HashMap<>();
        LdapUserMapping userMapping = new LdapUserMapping("ou=People,dc=example,dc=org",
                "(&(objectclass=posixAccount)(uid:caseExactMatch:={0}))",
                "uid",
                "cn"
        );
        userMappings.put("default", userMapping);
        Map<String, LdapGroupMapping> groupMappings = new HashMap<>();
        LdapGroupMapping groupMapping = new LdapGroupMapping("ou=group,dc=example,dc=org",
                "cn",
                "(&(objectclass=posixGroup)(memberUid:caseExactMatch:={0}))",
                new String[] { "uid" }
                );
        groupMappings.put("default", groupMapping);

        LdapRealm ldapRealm = new LdapRealm("test", contextFactories, userMappings, groupMappings);

        Authenticator.Context context = new Authenticator.Context("test", "test", null);
        boolean result = ldapRealm.getAuthenticator().authenticate(context);
        assertTrue(result);

        UsersProvider.Context userContext = new UsersProvider.Context("test", null);
        UserDetails userDetails = ldapRealm.getUsersProvider().getUserDetails(userContext);
        assertEquals("Test", userDetails.getName());
        assertEquals("test", userDetails.getUserId());

        GroupsProvider.Context groupContext = new GroupsProvider.Context("test", null);
        Collection<String> collection = ldapRealm.getGroupsProvider().getGroups(groupContext);
        assertEquals("[test]", collection.toString());
    }
}
