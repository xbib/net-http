package org.xbib.net.http.server.ldap;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class LdapContextFactory {

    private static final Logger logger = Logger.getLogger(LdapContextFactory.class.getName());

    public static final String AUTH_METHOD_GSSAPI = "GSSAPI";

    public static final String AUTH_METHOD_DIGEST_MD5 = "DIGEST-MD5";

    public static final String AUTH_METHOD_CRAM_MD5 = "CRAM-MD5";

    public static final String REFERRALS_FOLLOW_MODE = "follow";

    public static final String REFERRALS_IGNORE_MODE = "ignore";

    public static final String DEFAULT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    /**
     * The Sun LDAP property used to enable connection pooling.
     * This is used in the default implementation to enable LDAP connection pooling.
     */
    private static final String SUN_CONNECTION_POOLING_PROPERTY = "com.sun.jndi.ldap.connect.pool";

    private static final String SASL_REALM_PROPERTY = "java.naming.security.sasl.realm";

    private final String providerUrl;

    private final boolean startTLS;

    private final String authentication;

    private final String factory;

    private final String username;

    private final String password;

    private final String realm;

    private final String referral;

    public LdapContextFactory(String authentication,
                              String factory,
                              String realm,
                              String providerUrl,
                              boolean startTLS,
                              String username,
                              String password,
                              String referral) {
        this.authentication =  authentication;
        this.factory = factory;
        this.realm = realm;
        this.providerUrl = providerUrl;
        this.startTLS = startTLS;
        this.username = username;
        this.password = password;
        this.referral = referral;
    }

    public InitialDirContext createBindContext() throws NamingException {
        if (isGssapi()) {
            return createInitialDirContextUsingGssapi(username, password);
        } else {
            return createInitialDirContext(username, password, true);
        }
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public String getReferral() {
        return referral;
    }

    public InitialDirContext createUserContext(String principal, String credentials) throws NamingException {
        return createInitialDirContext(principal, credentials, false);
    }

    private InitialDirContext createInitialDirContext(String principal, String credentials, boolean pooling) throws NamingException {
        final InitialLdapContext ctx;
        if (startTLS) {
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
            env.put(Context.PROVIDER_URL, providerUrl);
            env.put(Context.REFERRAL, referral);
            logger.log(Level.FINE, "new initial LDAP context: " + env);
            ctx = new InitialLdapContext(env, null);
            // http://docs.oracle.com/javase/jndi/tutorial/ldap/ext/starttls.html
            StartTlsResponse tls = (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
            try {
                tls.negotiate();
            } catch (IOException e) {
                NamingException ex = new NamingException("StartTLS failed");
                ex.initCause(e);
                throw ex;
            }
            ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, authentication);
            if (principal != null) {
                ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, principal);
            }
            if (credentials != null) {
                ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, credentials);
            }
            ctx.reconnect(null);
        } else {
            Properties env = getEnvironment(principal, credentials, pooling);
            logger.log(Level.FINE, "new initial LDAP context: " + env);
            ctx = new InitialLdapContext(env, null);
        }
        return ctx;
    }

    private InitialDirContext createInitialDirContextUsingGssapi(String principal, String credentials) throws NamingException {
        Configuration.setConfiguration(new Krb5LoginConfiguration());
        InitialDirContext initialDirContext;
        try {
            LoginContext lc = new LoginContext(getClass().getName(), new CallbackHandlerImpl(principal, credentials));
            lc.login();
            initialDirContext = Subject.doAs(lc.getSubject(), (PrivilegedExceptionAction<InitialDirContext>) () -> {
                Properties env = new Properties();
                env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
                env.put(Context.PROVIDER_URL, providerUrl);
                env.put(Context.REFERRAL, referral);
                logger.log(Level.FINE, "new initial LDAP context: " + env);
                return new InitialLdapContext(env, null);
            });
        } catch (LoginException | PrivilegedActionException e) {
            NamingException namingException = new NamingException(e.getMessage());
            namingException.initCause(e);
            throw namingException;
        }
        return initialDirContext;
    }

    private Properties getEnvironment(String principal, String credentials, boolean pooling) {
        Properties env = new Properties();
        env.put(Context.SECURITY_AUTHENTICATION, authentication);
        if (realm != null) {
            env.put(SASL_REALM_PROPERTY, realm);
        }
        if (pooling) {
            env.put(SUN_CONNECTION_POOLING_PROPERTY, "true");
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.REFERRAL, referral);
        if (principal != null) {
            env.put(Context.SECURITY_PRINCIPAL, principal);
        }
        if (credentials != null) {
            env.put(Context.SECURITY_CREDENTIALS, credentials);
        }
        return env;
    }

    public boolean isSasl() {
        return AUTH_METHOD_DIGEST_MD5.equals(authentication) ||
                AUTH_METHOD_CRAM_MD5.equals(authentication) ||
                AUTH_METHOD_GSSAPI.equals(authentication);
    }

    public boolean isGssapi() {
        return AUTH_METHOD_GSSAPI.equals(authentication);
    }

    public void testConnection() {
        if (username.isBlank() && isSasl()) {
            throw new IllegalArgumentException("when using SASL, property bindDn is required");
        }
        try {
            createBindContext();
            logger.log(Level.INFO, "test LDAP connection on " + providerUrl + ": OK");
        } catch (NamingException e) {
            logger.info("test LDAP connection: FAIL");
            throw new LdapException("Unable to open LDAP connection", e);
        }
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "url=" + providerUrl +
                ", authentication=" + authentication +
                ", factory=" + factory +
                ", bindDn=" + username +
                ", realm=" + realm +
                ", referral=" + referral +
                "}";
    }
}
