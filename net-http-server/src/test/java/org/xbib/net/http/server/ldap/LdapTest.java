package org.xbib.net.http.server.ldap;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LdapTest {

    @Disabled
    @Test
    public void testLdap() {
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
        LdapUserMapping userMapping = new LdapUserMapping("ou=People.dc=example,dc=org",
                "(&(objectclass=posixAccount)(uid:caseExactMatch:={0}))",
                "uid",
                "cn"
                );
        userMappings.put("default", userMapping);

        LdapAuthenticator authenticator = new LdapAuthenticator(contextFactories, userMappings);
        boolean result = authenticator.authenticate("test", "test");
        assertTrue(result);
    }
}
