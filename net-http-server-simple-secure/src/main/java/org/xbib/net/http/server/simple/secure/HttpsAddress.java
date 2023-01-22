package org.xbib.net.http.server.simple.secure;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.security.CertificateProvider;
import org.xbib.net.security.CertificateReader;
import org.xbib.net.security.ssl.SSLFactory;
import org.xbib.net.security.util.DistinguishedNameParser;

import javax.net.ssl.SSLContext;
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

public class HttpsAddress extends HttpAddress {

    private final SSLContext sslContext;

    public HttpsAddress(String host, Integer port, HttpVersion version,
                        boolean secure, Set<String> hostNames, SSLContext sslContext) {
        super(host, port, version, secure, hostNames);
        this.sslContext = sslContext;
    }

    public static Builder builder() {
        return new Builder().setSecure(true);
    }

    public static HttpsAddress https1(String host) throws KeyStoreException {
        return builder()
                .setVersion(HttpVersion.HTTP_1_1)
                .setHost(host)
                .setPort(443)
                .build();
    }

    public static HttpAddress https1(String host, int port) throws KeyStoreException {
        return builder()
                .setVersion(HttpVersion.HTTP_1_1)
                .setHost(host)
                .setPort(port)
                .build();
    }

    public static HttpAddress https2(String host) throws KeyStoreException {
        return builder()
                .setVersion(HttpVersion.HTTP_2_0)
                .setHost(host)
                .setPort(443)
                .build();
    }

    public static HttpAddress https2(String host, int port) throws KeyStoreException {
        return builder()
                .setVersion(HttpVersion.HTTP_2_0)
                .setHost(host)
                .setPort(port)
                .build();
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public static class Builder extends HttpAddress.Builder {

        private static TrustManagerFactory TRUST_MANAGER_FACTORY;

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

        private Iterable<String> ciphers;

        private Collection<? extends X509Certificate> certChain;

        private PrivateKey privateKey;

        private String privateKeyPassword;

        private Set<String> hostNames;

        private Builder() {
            this.trustManagerFactory = TRUST_MANAGER_FACTORY;
            this.ciphers = DEFAULT_JDK_CIPHERS;
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

        public Builder setCiphers(Iterable<String> ciphers) {
            this.ciphers = ciphers;
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
            this.privateKeyPassword = password;
            for (CertificateProvider provider : certificateProviders) {
                try {
                    Map.Entry<PrivateKey, Collection<? extends X509Certificate>> entry =
                            provider.provide(keyInputStream, password, chain);
                    setPrivateKey(entry.getKey());
                    setCertChain(entry.getValue());
                    found = true;
                    break;
                } catch (CertificateException | IOException e) {
                    // ignore
                }
            }
            if (!found) {
                throw new CertificateException("no certificate found");
            }
            // automatic adding of certificate DNS names for automatic domain name match setup
            List<X509Certificate> certificates = CertificateReader.orderCertificateChain(certChain);
            hostNames =  getServerNames(certificates.get(0));
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

        public HttpsAddress build() throws KeyStoreException {
            Objects.requireNonNull(host);
            Objects.requireNonNull(version);
            Objects.requireNonNull(privateKey);
            Objects.requireNonNull(certChain);
            if (certChain.isEmpty()) {
                throw new IllegalArgumentException("cert chain must not be empty");
            }
            Objects.requireNonNull(ciphers);
            // trustManagerKeyStore may be null, this will be used to init() for default behavior
            trustManagerFactory.init(trustManagerKeyStore);
            SSLFactory sslFactory = SSLFactory.builder()
                    .withCiphers(ciphers)
                    .withIdentityMaterial(privateKey,
                            privateKeyPassword != null ? privateKeyPassword.toCharArray() : null,
                            certChain)
                    .withTrustMaterial(trustManagerFactory)
                    .build();
            return new HttpsAddress(host, port, version, isSecure, hostNames, sslFactory.getSslContext());
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
