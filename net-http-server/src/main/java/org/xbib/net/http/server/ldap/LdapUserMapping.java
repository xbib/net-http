package org.xbib.net.http.server.ldap;

public class LdapUserMapping {

    private final String baseDn;

    private final String filter;

    private final String uidAttribute;

    private final String realNameAttribute;

    public LdapUserMapping(String baseDn,
                           String filter,
                           String uidAttribute,
                           String realNameAttribute) {
        this.baseDn = baseDn;
        this.filter = filter;
        this.uidAttribute = uidAttribute;
        this.realNameAttribute = realNameAttribute;
    }

    public LdapSearch createSearch(LdapContextFactory contextFactory, String username) {
        return new LdapSearch(contextFactory)
                .setBaseDn(baseDn)
                .setFilter(filter)
                .setFilterArgs(username)
                .returns(uidAttribute, realNameAttribute);
    }

    public String getUidAttribute() {
        return uidAttribute;
    }

    public String getRealNameAttribute() {
        return realNameAttribute;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "baseDn=" + baseDn +
                ",filter=" + filter +
                ",uidAttribute=" + uidAttribute +
                ",realNameAttribute=" + realNameAttribute +
                "}";
    }

}
