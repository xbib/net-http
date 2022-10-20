package org.xbib.net.http.server.netty.secure;

import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;
import io.netty.util.DomainWildcardMappingBuilder;
import io.netty.util.Mapping;
import java.security.AlgorithmConstraints;
import java.util.Optional;
import java.util.ServiceLoader;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.HttpDomain;

import java.util.Collection;

public class NettyHttpsServerConfig extends NettyHttpServerConfig {

    public static final AttributeKey<ServerNameIndicationHandler> ATTRIBUTE_KEY_SNI_HANDLER = AttributeKey.valueOf("_sni_handler");

    private static final String[] DEFAULT_PROTOCOLS = new String[] { "TLSv1.2", "TLSv1.3"}; // prefer TLSv1.2

    private static ServerSecureSocketProvider serverSecureSocketProvider;

    static {
        ServiceLoader<ServerSecureSocketProvider> serviceLoader = ServiceLoader.load(ServerSecureSocketProvider.class);
        Optional<ServerSecureSocketProvider> optional = serviceLoader.findFirst();
        serverSecureSocketProvider = optional.orElse(new JdkServerSecureSocketProvider());
    }

    private Mapping<String, SslContext> domainNameMapping;

    private String[] protocols;

    private String[] cipherSuites;

    private boolean isUseCipherSuiteOrdered;

    private boolean isRetransmissionEnabled;

    private int maximumPacketSize;

    private AlgorithmConstraints algorithmConstraints;

    private boolean needsClientAuth;

    private boolean wantsClientAuth;

    public NettyHttpsServerConfig() {
        this.isRetransmissionEnabled = true;
        this.maximumPacketSize = 0;
        this.isUseCipherSuiteOrdered = true;
        this.algorithmConstraints = null;
        this.needsClientAuth = false;
        this.wantsClientAuth = false;
    }

    public static void setServerSecureSocketProvider(ServerSecureSocketProvider serverSecureSocketProvider) {
        NettyHttpsServerConfig.serverSecureSocketProvider = serverSecureSocketProvider;
    }

    public ServerSecureSocketProvider getServerSecureSocketProvider() {
        return serverSecureSocketProvider;
    }

    public Mapping<String, SslContext> getDomainNameMapping(Collection<HttpDomain> domains) {
        if (domainNameMapping == null) {
            buildMapping(domains);
        }
        return domainNameMapping;
    }

    public void setProtocols(String[] protocols) {
        this.protocols = protocols;
    }

    public String[] getProtocols(HttpAddress httpAddress) {
        if (protocols == null) {
            protocols = serverSecureSocketProvider.protocols(httpAddress);
        }
        if (protocols == null) {
            protocols = DEFAULT_PROTOCOLS;
        }
        return protocols;
    }

    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public String[] getCipherSuites(HttpAddress httpAddress) {
        return cipherSuites;
    }

    public void setUseCipherSuiteOrdered(boolean useCipherSuiteOrdered) {
        isUseCipherSuiteOrdered = useCipherSuiteOrdered;
    }

    public boolean isUseCipherSuiteOrdered() {
        return isUseCipherSuiteOrdered;
    }

    public void setMaximumPacketSize(int maximumPacketSize) {
        this.maximumPacketSize = maximumPacketSize;
    }

    public int getMaximumPacketSize() {
        return maximumPacketSize;
    }

    public void setRetransmissionEnabled(boolean retransmissionEnabled) {
        isRetransmissionEnabled = retransmissionEnabled;
    }

    public boolean isRetransmissionEnabled() {
        return isRetransmissionEnabled;
    }

    public void setAlgorithmConstraints(AlgorithmConstraints algorithmConstraints) {
        this.algorithmConstraints = algorithmConstraints;
    }

    public AlgorithmConstraints getAlgorithmConstraints() {
        return algorithmConstraints;
    }

    public void setNeedsClientAuth(boolean needsClientAuth) {
        this.needsClientAuth = needsClientAuth;
    }

    public boolean isNeedsClientAuth() {
        return needsClientAuth;
    }

    public void setWantsClientAuth(boolean wantsClientAuth) {
        this.wantsClientAuth = wantsClientAuth;
    }

    public boolean isWantsClientAuth() {
        return wantsClientAuth;
    }

    private void buildMapping(Collection<HttpDomain> domains) {
        if (domains.isEmpty()) {
            throw new IllegalStateException("no domains found for domain name mapping");
        }
        // use first domain as default domain for SSL context
        SslContext defaultContext = getSslContextFrom(domains.iterator().next());
        DomainWildcardMappingBuilder<SslContext> mappingBuilder = new DomainWildcardMappingBuilder<>(defaultContext);
        for (HttpDomain httpDomain : domains) {
            SslContext sslContext = getSslContextFrom(httpDomain);
            HttpAddress httpAddress = httpDomain.getAddress();
            if (httpAddress.getHostNames() != null) {
                for (String name : httpAddress.getHostNames()) {
                    mappingBuilder.add(name + ":" + httpAddress.getPort(), sslContext);
                }
            }
            for (String name : httpDomain.getNames()) {
                mappingBuilder.add(name, sslContext);
            }
        }
        domainNameMapping = mappingBuilder.build();
    }

    public SslContext getSslContextFrom(HttpDomain httpDomain) {
        HttpAddress httpAddress = httpDomain.getAddress();
        if (httpAddress instanceof HttpsAddress) {
            HttpsAddress httpsAddress = (HttpsAddress) httpAddress;
            return httpsAddress.getSslContext();
        }
        throw new IllegalStateException("no secure http, no SslContext configured for domain " + httpDomain);
    }
}
