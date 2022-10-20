package org.xbib.net.http.server.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

public class LdapGroupMapping {

    private static final String DEFAULT_OBJECT_CLASS = "groupOfUniqueNames";
    private static final String DEFAULT_ID_ATTRIBUTE = "cn";
    private static final String DEFAULT_MEMBER_ATTRIBUTE = "uniqueMember";
    private static final String DEFAULT_FILTER = "(&(objectClass=groupOfUniqueNames)(uniqueMember={dn}))";

    private final String baseDn;

    private final String idAttribute;

    private final String filter;

    private final String[] filterArgNames;

    public LdapGroupMapping(String baseDn,
                            String idAttribute,
                            String filter,
                            String[] filterArgNames) {
        this.baseDn = baseDn;
        this.idAttribute = idAttribute;
        this.filter = filter;
        this.filterArgNames = filterArgNames;
    }

    /**
     * Search for this mapping.
     */
    public LdapSearch createSearch(LdapContextFactory contextFactory, SearchResult searchResult) {
        String[] filterArgs = new String[filterArgNames.length];
        for (int i = 0; i < filterArgs.length; i++) {
            String name = filterArgNames[i];
            if ("dn".equals(name)) {
                filterArgs[i] = searchResult.getNameInNamespace();
            } else {
                filterArgs[i] = getAttributeValue(searchResult, name);
            }
        }
        return new LdapSearch(contextFactory)
                .setBaseDn(baseDn)
                .setFilter(filter)
                .setFilterArgs(filterArgs)
                .returns(idAttribute);
    }

    private static String getAttributeValue(SearchResult user, String attributeId) {
        Attribute attribute = user.getAttributes().get(attributeId);
        if (attribute == null) {
            return null;
        }
        try {
            return (String) attribute.get();
        } catch (NamingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getIdAttribute() {
        return idAttribute;
    }

    public String[] getFilterArgNames() {
        return filterArgNames;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "baseDn=" + baseDn +
                ", idAttribute=" + idAttribute +
                ", filter=" + filter +
                ", filterArgs=" + filterArgNames +
                "}";
    }
}
