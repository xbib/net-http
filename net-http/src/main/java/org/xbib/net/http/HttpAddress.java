package org.xbib.net.http;

import java.security.KeyStoreException;
import java.util.Objects;
import javax.net.ssl.SSLException;
import org.xbib.net.Address;
import org.xbib.net.NetworkUtils;
import org.xbib.net.SocketConfig;
import org.xbib.net.URL;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

/**
 * An address handle for host, port, HTTP version, secure transport flag of a channel for HTTP.
 */
public class HttpAddress implements Address {

    private final Builder builder;

    private InetAddress inetAddress;

    private InetSocketAddress inetSocketAddress;

    public static Builder builder() {
        return new Builder();
    }

    public static HttpAddress http1(String host) {
        return new HttpAddress(host, 80, HttpVersion.HTTP_1_1, false);
    }

    public static HttpAddress http1(String host, int port) {
        return new HttpAddress(host, port, HttpVersion.HTTP_1_1, false);
    }

    public static HttpAddress http2(String host) {
        return new HttpAddress(host, 443, HttpVersion.HTTP_2_0, false);
    }

    public static HttpAddress http2(String host, int port) {
        return new HttpAddress(host, port, HttpVersion.HTTP_2_0, false);
    }

    public static HttpAddress http1(URL url) {
        return new HttpAddress(url, HttpVersion.HTTP_1_1);
    }

    public static HttpAddress http2(URL url) {
        return new HttpAddress(url, HttpVersion.HTTP_2_0);
    }

    public static HttpAddress of(URL url) {
        return new HttpAddress(url, HttpVersion.HTTP_1_1);
    }

    public static HttpAddress of(URL url, HttpVersion httpVersion) {
        return new HttpAddress(url, httpVersion);
    }

    public static HttpAddress of(String host, Integer port, HttpVersion version, boolean secure) {
        return new HttpAddress(host, port, version, secure, Set.of());
    }

    public HttpAddress(URL url, HttpVersion version) {
        this(url.getHost(), url.getPort(), version, "https".equals(url.getScheme()), Set.of());
    }

    public HttpAddress(String host, Integer port, HttpVersion version, boolean secure) {
        this(host, port, version, secure, Set.of());
    }

    public HttpAddress(String host, Integer port, HttpVersion version,
                       boolean secure, Set<String> hostNames) {
        this(builder()
                .setHost(host)
                .setPort((port == null || port == -1) ? secure ? 443 : 80 : port)
                .setVersion(version)
                .setSecure(secure)
                .setHostNames(hostNames)
                .setSocketConfig(new SocketConfig()));
    }

    public HttpAddress(Builder builder) {
        this.builder = builder;
    }

    @Override
    public String getHost() {
        return builder.host;
    }

    @Override
    public Integer getPort() {
        return builder.port;
    }

    @Override
    public InetAddress getInetAddress() throws IOException {
        if (inetAddress == null) {
            this.inetAddress = NetworkUtils.resolveInetAddress(builder.host, null);
        }
        return inetAddress;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() throws IOException {
        if (inetSocketAddress == null) {
            InetAddress inetAddress = getInetAddress();
            this.inetSocketAddress = new InetSocketAddress(inetAddress.getHostAddress(), builder.port);
        }
        return inetSocketAddress;
    }

    @Override
    public URL base() {
        return isSecure() ?
                URL.https().host(builder.host).port(builder.port).build() :
                URL.http().host(builder.host).port(builder.port).build();
    }

    @Override
    public boolean isSecure() {
        return builder.isSecure;
    }

    @Override
    public SocketConfig getSocketConfig() {
        return builder.socketConfig;
    }

    public Set<String> getHostNames() {
        return builder.hostNames;
    }

    public HttpVersion getVersion() {
        return builder.version;
    }

    public String hostAndPort() {
        return builder.host + ":" + builder.port;
    }

    public String hostAddressAndPort() throws IOException {
        return getInetAddress().getHostAddress() + ":" + builder.port;
    }

    public String canonicalHostAndPort() throws IOException {
        return getInetAddress().getCanonicalHostName() + ":" + builder.port;
    }

    @Override
    public String toString() {
        return "[" + builder.version + "]" + (builder.isSecure ? "[SECURE]" : "") + builder.host + ":" + builder.port;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof HttpAddress &&
                builder.host.equals(((HttpAddress) object).builder.host) &&
                (builder.port != null && builder.port.equals(((HttpAddress) object).builder.port)) &&
                builder.version.equals(((HttpAddress) object).builder.version) &&
                builder.isSecure.equals(((HttpAddress) object).builder.isSecure);
    }

    @Override
    public int hashCode() {
        return builder.host.hashCode() ^ builder.port ^ builder.version.hashCode() ^ builder.isSecure.hashCode();
    }

    public static class Builder {

        protected String host;

        protected Integer port;

        protected Boolean isSecure;

        protected HttpVersion version;

        protected Set<String> hostNames;

        protected SocketConfig socketConfig;

        protected Builder() {
            this.port = -1;
            this.isSecure = false;
            this.version = HttpVersion.HTTP_1_1;
            this.socketConfig = new SocketConfig();
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setSecure(boolean secure) {
            this.isSecure = secure;
            return this;
        }

        public Builder setVersion(HttpVersion httpVersion) {
            this.version = httpVersion;
            return this;
        }

        public Builder setHostNames(Set<String> hostNames) {
            this.hostNames = hostNames;
            return this;
        }

        public Builder setSocketConfig(SocketConfig socketConfig) {
            this.socketConfig = socketConfig;
            return this;
        }

        public HttpAddress build() throws KeyStoreException, SSLException {
            Objects.requireNonNull(host);
            Objects.requireNonNull(version);
            return new HttpAddress(this);
        }
    }
}
