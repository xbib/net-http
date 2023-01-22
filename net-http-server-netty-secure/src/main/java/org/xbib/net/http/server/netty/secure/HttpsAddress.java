package org.xbib.net.http.server.netty.secure;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.xbib.net.security.CertificateProvider;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.security.CertificateReader;
import org.xbib.net.security.util.DistinguishedNameParser;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpsAddress extends HttpAddress {

    private static final Logger logger = Logger.getLogger(HttpsAddress.class.getName());

    private final SslContext sslContext;

    public HttpsAddress(String host, Integer port, HttpVersion version,
                        boolean secure, Set<String> hostNames, SslContext sslContext) {
        super(host, port, version, secure, hostNames);
        this.sslContext = sslContext;
    }

    public static Builder builder() {
        return new Builder().setSecure(true);
    }

    public static HttpsAddress https1(String host) throws KeyStoreException, SSLException {
        return builder()
                .setVersion(HttpVersion.HTTP_1_1)
                .setHost(host)
                .setPort(443)
                .build();
    }

    public static HttpAddress https1(String host, int port) throws KeyStoreException, SSLException {
        return builder()
                .setVersion(HttpVersion.HTTP_1_1)
                .setHost(host)
                .setPort(port)
                .build();
    }

    public static HttpAddress https2(String host) throws KeyStoreException, SSLException {
        return builder()
                .setVersion(HttpVersion.HTTP_2_0)
                .setHost(host)
                .setPort(443)
                .build();
    }

    public static HttpAddress https2(String host, int port) throws KeyStoreException, SSLException {
        return builder()
                .setVersion(HttpVersion.HTTP_2_0)
                .setHost(host)
                .setPort(port)
                .build();
    }

    public SslContext getSslContext() {
        return sslContext;
    }

    public static class Builder extends HttpAddress.Builder {

        private static TrustManagerFactory TRUST_MANAGER_FACTORY;

        private static final Iterable<String> DEFAULT_OPENSSL_CIPHERS = Arrays.asList(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256"
        );

        private static final Iterable<String> DEFAULT_JDK_CIPHERS =
                Arrays.asList(((SSLSocketFactory) SSLSocketFactory.getDefault()).getDefaultCipherSuites());

        static {
            try {
                TRUST_MANAGER_FACTORY = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            } catch (Exception e) {
                TRUST_MANAGER_FACTORY = null;
            }
        }

        private TrustManagerFactory trustManagerFactory;

        private KeyStore trustManagerKeyStore;

        private Provider sslContextProvider;

        private SslProvider sslProvider;

        private Iterable<String> ciphers;

        private CipherSuiteFilter cipherSuiteFilter;

        private Collection<? extends X509Certificate> certChain;

        private PrivateKey privateKey;

        private ApplicationProtocolConfig applicationProtocolConfig;

        private long sesseionCacheSize = 0L;

        private long sessionTimeout = 0L;

        private ClientAuth clientAuth = ClientAuth.NONE;

        private boolean enableOcsp;

        protected Builder() {
            this.trustManagerFactory = TRUST_MANAGER_FACTORY;
            this.sslProvider = OpenSsl.isAvailable() ? SslProvider.OPENSSL : SslProvider.JDK;
            this.ciphers = OpenSsl.isAvailable() ? DEFAULT_OPENSSL_CIPHERS : DEFAULT_JDK_CIPHERS;
            this.cipherSuiteFilter = SupportedCipherSuiteFilter.INSTANCE;
        }

        @Override
        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        @Override
        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        @Override
        public Builder setSecure(boolean secure) {
            this.isSecure = secure;
            return this;
        }

        @Override
        public Builder setVersion(HttpVersion version) {
            this.version = version;
            return this;
        }

        public Builder setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
            this.trustManagerFactory = trustManagerFactory;
            return this;
        }

        public Builder setTrustManagerKeyStore(KeyStore trustManagerKeyStore) {
            this.trustManagerKeyStore = trustManagerKeyStore;
            return this;
        }

        public Builder setSslContextProvider(Provider sslContextProvider) {
            this.sslContextProvider = sslContextProvider;
            return this;
        }

        public Builder setSslProvider(SslProvider sslProvider) {
            this.sslProvider = sslProvider;
            return this;
        }

        public Builder setCiphers(Iterable<String> ciphers) {
            this.ciphers = ciphers;
            return this;
        }

        public Builder setCipherSuiteFilter(CipherSuiteFilter cipherSuiteFilter) {
            this.cipherSuiteFilter = cipherSuiteFilter;
            return this;
        }

        public Builder setJdkSslProvider() {
            setSslProvider(SslProvider.JDK);
            setCiphers(DEFAULT_JDK_CIPHERS);
            return this;
        }

        public Builder setOpenSSLSslProvider() {
            setSslProvider(SslProvider.OPENSSL);
            setCiphers(DEFAULT_OPENSSL_CIPHERS);
            return this;
        }

        public Builder setPrivateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder setCertChain(Collection<? extends X509Certificate> chain) {
            Objects.requireNonNull(chain);
            this.certChain = chain;
            return this;
        }

        public Builder setCertChain(InputStream keyInputStream, String password, InputStream chain)
                throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
            ServiceLoader<CertificateProvider> certificateProviders = ServiceLoader.load(CertificateProvider.class);
            boolean found = false;
            for (CertificateProvider provider : certificateProviders) {
                try {
                    Map.Entry<PrivateKey, Collection<? extends X509Certificate>> entry =
                            provider.provide(keyInputStream, password, chain);
                    if (entry != null) {
                        setPrivateKey(entry.getKey());
                        setCertChain(entry.getValue());
                        found = true;
                        break;
                    }
                } catch (CertificateException | IOException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
            if (!found) {
                throw new CertificateException("no certificate found");
            }
            // automatic adding of certificate DNS names for automatic domain name match setup
            List<X509Certificate> certificates = CertificateReader.orderCertificateChain(certChain);
            hostNames = getServerNames(certificates.get(0));
            return this;
        }

        public Builder setSelfCert(String fullQualifiedDomainName) throws CertificateException {
            ServiceLoader<CertificateProvider> certificateProviders = ServiceLoader.load(CertificateProvider.class);
            boolean found = false;
            for (CertificateProvider provider : certificateProviders) {
                try {
                    Map.Entry<PrivateKey, Collection<? extends X509Certificate>> entry =
                        provider.provideSelfSigned(fullQualifiedDomainName);
                    setPrivateKey(entry.getKey());
                    setCertChain(entry.getValue());
                    found = true;
                } catch (CertificateException | IOException e) {
                    // ignore
                }
            }
            if (!found) {
                throw new CertificateException("no self-signed certificate found");
            }
            return this;
        }

        public Builder setApplicationProtocolConfig(ApplicationProtocolConfig applicationProtocolConfig) {
            this.applicationProtocolConfig = applicationProtocolConfig;
            return this;
        }

        public Builder setSessionCacheSize(long sessionCacheSize) {
            this.sesseionCacheSize = sessionCacheSize;
            return this;
        }

        public Builder setSessionTimeout(long sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        /**
         * NONE, OPTIONAL, REQUIRE.
         * @param clientAuth the client auth mode
         * @return this builder
         */
        public Builder setClientAuth(ClientAuth clientAuth) {
            this.clientAuth = clientAuth;
            return this;
        }

        public Builder enableOcsp(boolean enableOcsp) {
            this.enableOcsp = enableOcsp;
            return this;
        }

        @Override
        public HttpsAddress build() throws KeyStoreException, SSLException {
            Objects.requireNonNull(host);
            Objects.requireNonNull(version);
            Objects.requireNonNull(privateKey);
            Objects.requireNonNull(certChain);
            if (certChain.isEmpty()) {
                throw new IllegalArgumentException("cert chain must not be empty");
            }
            Objects.requireNonNull(sslProvider);
            Objects.requireNonNull(ciphers);
            Objects.requireNonNull(cipherSuiteFilter);
            // trustManagerKeyStore may be null, this will be used to init() for default behavior
            trustManagerFactory.init(trustManagerKeyStore);
            SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(privateKey, certChain)
                    .trustManager(trustManagerFactory)
                    .sslProvider(sslProvider)
                    .ciphers(ciphers, cipherSuiteFilter);
            if (sslContextProvider != null) {
                sslContextBuilder.sslContextProvider(sslContextProvider);
            }
            if (applicationProtocolConfig == null) {
                if (version.equals(HttpVersion.HTTP_2_0)) {
                    // OpenSSL does not support FATAL_ALERT behaviour
                    applicationProtocolConfig = new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1);
                }
                if (version.equals(HttpVersion.HTTP_1_1)) {
                    applicationProtocolConfig = new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_1_1);
                }
            }
            sslContextBuilder.applicationProtocolConfig(applicationProtocolConfig);
            sslContextBuilder.sessionCacheSize(sesseionCacheSize);
            sslContextBuilder.sessionTimeout(sessionTimeout);
            sslContextBuilder.clientAuth(clientAuth);
            sslContextBuilder.enableOcsp(enableOcsp);
            SslContext sslContext = sslContextBuilder.build();
            logger.log(Level.FINE, "SSL context up: " + sslContext.getClass().getName() +
                            " negotiating for protocols = " + sslContext.applicationProtocolNegotiator().protocols() +
                            " session cache = " + sslContext.sessionCacheSize() +
                            " session timeout = " + sslContext.sessionTimeout() +
                            " cipher suite = " + sslContext.cipherSuites()
                    );
            return new HttpsAddress(host, port, version, isSecure, hostNames, sslContext);
        }
    }

    private static Set<String> getServerNames(X509Certificate certificate) throws CertificateParsingException {
        Set<String> set = new LinkedHashSet<>();
        set.add(new DistinguishedNameParser(certificate.getSubjectX500Principal()).findMostSpecific("CN"));
        Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
        if (altNames != null) {
            for (List<?> altName : altNames) {
                Integer type = (Integer) altName.get(0);
                if (type == 2) { // Type = DNS
                    String string = altName.get(1).toString();
                    set.add(string);
                }
            }
        }
        return set;
    }
}
