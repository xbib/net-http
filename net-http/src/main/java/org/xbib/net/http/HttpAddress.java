package org.xbib.net.http;

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

    private final String host;

    private final Integer port;

    private final HttpVersion version;

    private final Boolean secure;

    private final Set<String> hostNames;

    private InetAddress inetAddress;

    private InetSocketAddress inetSocketAddress;

    private SocketConfig socketConfig;

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
        this.host = host;
        this.port = (port == null || port == -1) ? secure ? 443 : 80 : port;
        this.version = version;
        this.secure = secure;
        this.hostNames = hostNames;
        this.socketConfig = new SocketConfig();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public InetAddress getInetAddress() throws IOException {
        if (inetAddress == null) {
            this.inetAddress = NetworkUtils.resolveInetAddress(host, null);
        }
        return inetAddress;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() throws IOException {
        if (inetSocketAddress == null) {
            InetAddress inetAddress = getInetAddress();
            this.inetSocketAddress = new InetSocketAddress(inetAddress.getHostAddress(), port);
        }
        return inetSocketAddress;
    }

    @Override
    public URL base() {
        return isSecure() ?
                URL.https().host(host).port(port).build() :
                URL.http().host(host).port(port).build();
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override
    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public HttpVersion getVersion() {
        return version;
    }

    public String hostAndPort() {
        return host + ":" + port;
    }

    public String hostAddressAndPort() throws IOException {
        return getInetAddress().getHostAddress() + ":" + port;
    }

    public String canonicalHostAndPort() throws IOException {
        return getInetAddress().getCanonicalHostName() + ":" + port;
    }

    @Override
    public String toString() {
        return "[" + version + "]" + (secure ? "[SECURE]" : "") + host + ":" + port;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof HttpAddress &&
                host.equals(((HttpAddress) object).host) &&
                (port != null && port.equals(((HttpAddress) object).port)) &&
                version.equals(((HttpAddress) object).version) &&
                secure.equals(((HttpAddress) object).secure);
    }

    @Override
    public int hashCode() {
        return host.hashCode() ^ port ^ version.hashCode() ^ secure.hashCode();
    }
}
