package org.xbib.net.http.server.ldap;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class LdapSearch {

    private static final Logger logger = Logger.getLogger(LdapSearch.class.getName());

    private final LdapContextFactory contextFactory;

    private String baseDn;

    private int scope = SearchControls.SUBTREE_SCOPE;

    private String filter;

    private String[] filterArgs;

    private String[] returningAttributes;

    public LdapSearch(LdapContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public LdapSearch setBaseDn(String baseDn) {
        this.baseDn = baseDn;
        return this;
    }

    public LdapSearch setScope(int scope) {
        this.scope = scope;
        return this;
    }

    public LdapSearch setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public LdapSearch setFilterArgs(String... filterArgs) {
        this.filterArgs = filterArgs;
        return this;
    }

    public LdapSearch returns(String... returningAttributes) {
        this.returningAttributes = returningAttributes;
        return this;
    }

    /**
     * Find results.
     * @throws NamingException if unable to perform search
     */
    public NamingEnumeration<SearchResult> find() throws NamingException {
        logger.log(Level.FINE, "find: " + this);
        NamingEnumeration<SearchResult> result;
        InitialDirContext context = null;
        boolean ok = false;
        try {
            context = contextFactory.createBindContext();
            SearchControls controls = new SearchControls();
            controls.setSearchScope(scope);
            controls.setReturningAttributes(returningAttributes);
            result = context.search(baseDn, filter, filterArgs, controls);
            logger.log(Level.FINE, "result = " + result + " hasMore = " + result.hasMore());
            ok = true;
        } finally {
            close(context, ok);
        }
        return result;
    }

    /**
     * Find unique.
     * @return result, or null if not found
     * @throws NamingException if unable to perform search, or non unique result
     */
    public SearchResult findUnique() throws NamingException {
        logger.log(Level.FINE, "find unique: " + this);
        NamingEnumeration<SearchResult> result = find();
        if (hasMore(result)) {
            SearchResult obj = result.next();
            if (!hasMore(result)) {
                logger.log(Level.FINE, "find unique result = " + obj);
                return obj;
            }
            throw new NamingException("Non unique result");
        }
        logger.log(Level.FINE, "find unique: no results");
        return null;
    }

    private static boolean hasMore(NamingEnumeration<SearchResult> result) throws NamingException {
        try {
            return result.hasMore();
        } catch (PartialResultException e) {
            logger.log(Level.FINE, "more result might be forthcoming if the referral is followed", e);
            // See http://docs.oracle.com/javase/jndi/tutorial/ldap/referral/jndi.html :
            // When the LDAP service provider receives a referral despite your having set Context.REFERRAL to "ignore", it will throw a
            // PartialResultException(in the API reference documentation) to indicate that more results might be forthcoming if the referral is
            // followed. In this case, the server does not support the Manage Referral control and is supporting referral updates in some other
            // way.
            return false;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "baseDn=" + baseDn +
                ", scope=" + scopeToString() +
                ", filter=" + filter +
                ", filterArgs=" + Arrays.toString(filterArgs) +
                ", returningAttributes=" + Arrays.toString(returningAttributes) +
                "}";
    }

    private String scopeToString() {
        switch (scope) {
            case SearchControls.ONELEVEL_SCOPE:
                return "onelevel";
            case SearchControls.OBJECT_SCOPE:
                return "object";
            case SearchControls.SUBTREE_SCOPE:
            default:
                return "subtree";
        }
    }

    /**
     * <pre>
     * public void useContextNicely() throws NamingException {
     *   InitialDirContext context = null;
     *   boolean threw = true;
     *   try {
     *     context = new InitialDirContext();
     *     // Some code which does something with the Context and may throw a NamingException
     *     threw = false; // No throwable thrown
     *   } finally {
     *     // Close context
     *     // If an exception occurs, only rethrow it if (threw==false)
     *     close(context, threw);
     *   }
     * }
     * </pre>
     *
     * @param context the {@code Context} object to be closed, or null, in which case this method does nothing
     * @param swallowIOException if true, don't propagate {@code NamingException} thrown by the {@code close} method
     * @throws NamingException if {@code swallowIOException} is false and {@code close} throws a {@code NamingException}.
     */
    private static void close(Context context, boolean swallowIOException) throws NamingException {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (NamingException e) {
            if (swallowIOException) {
                logger.log(Level.WARNING, "NamingException thrown while closing context.", e);
            } else {
                throw e;
            }
        }
    }
}
