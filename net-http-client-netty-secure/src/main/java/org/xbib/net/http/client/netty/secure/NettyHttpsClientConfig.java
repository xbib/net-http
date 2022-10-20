package org.xbib.net.http.client.netty.secure;

import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.TrustManagerFactory;
import org.xbib.net.http.client.ClientAuthMode;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;

public class NettyHttpsClientConfig extends NettyHttpClientConfig {

    private static final Logger logger = Logger.getLogger(NettyHttpsClientConfig.class.getName());

    public static final AttributeKey<SslHandler> ATTRIBUTE_KEY_SSL_HANDLER = AttributeKey.valueOf("_ssl_handler");

    private static TrustManagerFactory TRUST_MANAGER_FACTORY;

    static {
        try {
            TRUST_MANAGER_FACTORY = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (Exception e) {
            TRUST_MANAGER_FACTORY = null;
        }
    }

    private TrustManagerFactory trustManagerFactory = TRUST_MANAGER_FACTORY;

    private String secureSocketProviderName = "JDK";

    private KeyStore trustManagerKeyStore = null;

    private ClientAuthMode clientAuthMode = ClientAuthMode.NONE;

    private InputStream keyCertChainInputStream;

    private InputStream keyInputStream;

    private String keyPassword;

    private boolean protocolNegotiationEnabled = false;

    /*
     * Automatically selects the protocol from our secure socket providers.
     */
    private String[] secureProtocolName = null;

    public NettyHttpsClientConfig() {
    }

    public NettyHttpsClientConfig setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
        this.trustManagerFactory = trustManagerFactory;
        return this;
    }

    public NettyHttpsClientConfig trustInsecure() {
        this.trustManagerFactory = InsecureTrustManagerFactory.INSTANCE;
        return this;
    }

    public TrustManagerFactory getTrustManagerFactory() {
        initializeTrustManagerFactory();
        return trustManagerFactory;
    }

    public NettyHttpsClientConfig setTrustManagerKeyStore(KeyStore trustManagerKeyStore) {
        this.trustManagerKeyStore = trustManagerKeyStore;
        return this;
    }

    public KeyStore getTrustManagerKeyStore() {
        return trustManagerKeyStore;
    }

    public NettyHttpsClientConfig setSecureSocketProviderName(String secureSocketProviderName) {
        this.secureSocketProviderName = secureSocketProviderName;
        return this;
    }

    public String getSecureSocketProviderName() {
        return secureSocketProviderName;
    }

    public NettyHttpsClientConfig setSecureProtocolName(String[] secureProtocolName) {
        this.secureProtocolName = secureProtocolName;
        return this;
    }

    public String[] getSecureProtocolName() {
        return secureProtocolName;
    }

    public NettyHttpsClientConfig setKeyCert(InputStream keyCertChainInputStream,
                                             InputStream keyInputStream) {
        this.keyCertChainInputStream = keyCertChainInputStream;
        this.keyInputStream = keyInputStream;
        return this;
    }

    public InputStream getKeyCertChainInputStream() {
        return keyCertChainInputStream;
    }

    public InputStream getKeyInputStream() {
        return keyInputStream;
    }

    public NettyHttpsClientConfig setKeyCert(InputStream keyCertChainInputStream,
                                             InputStream keyInputStream,
                                             String keyPassword) {
        this.keyCertChainInputStream = keyCertChainInputStream;
        this.keyInputStream = keyInputStream;
        this.keyPassword = keyPassword;
        return this;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public NettyHttpsClientConfig setClientAuthMode(ClientAuthMode clientAuthMode) {
        this.clientAuthMode = clientAuthMode;
        return this;
    }

    public ClientAuthMode getClientAuthMode() {
        return clientAuthMode;
    }

    public NettyHttpsClientConfig setProtocolNegotiation(boolean negotiationEnabled) {
        this.protocolNegotiationEnabled = negotiationEnabled;
        return this;
    }

    public boolean isProtocolNegotiationEnabled() {
        return protocolNegotiationEnabled;
    }

    /**
     * Initialize trust manager factory once per client lifecycle.
     */
    private void initializeTrustManagerFactory() {
        if (trustManagerFactory != null) {
            try {
                trustManagerFactory.init(trustManagerKeyStore);
                logger.log(Level.FINE, "trust manager factory initialized with key store " + trustManagerFactory);
            } catch (KeyStoreException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        } else {
            logger.log(Level.INFO, "no trust manager factory present");
        }
    }
}
