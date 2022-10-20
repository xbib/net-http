package org.xbib.net.http.server;

import org.xbib.net.Parameter;
import org.xbib.net.URL;
import org.xbib.net.URLBuilder;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public abstract class BaseHttpRequestBuilder implements HttpRequestBuilder {

    HttpServerContext httpServerContext;

    HttpAddress httpAddress;

    InetSocketAddress localAddress;

    InetSocketAddress remoteAddress;

    URL serverURL;

    URL baseURL;

    String requestPath;

    Parameter parameter;

    Integer sequenceId;

    Integer streamId;

    Long requestId;

    HttpVersion httpVersion;

    HttpMethod httpMethod;

    HttpHeaders httpHeaders;

    String requestURI;

    ByteBuffer byteBuffer;

    protected BaseHttpRequestBuilder() {
        this.httpHeaders = new HttpHeaders();
    }

    @Override
    public BaseHttpRequestBuilder setContext(HttpServerContext httpServerContext) {
        this.httpServerContext = httpServerContext;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return this;
    }

    public HttpVersion getVersion() {
        return httpVersion;
    }

    @Override
    public BaseHttpRequestBuilder setMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public HttpMethod getMethod() {
        return httpMethod;
    }

    @Override
    public BaseHttpRequestBuilder setRequestURI(String requestURI) {
        this.requestURI = requestURI;
        return this;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public BaseHttpRequestBuilder setHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }

    @Override
    public HttpHeaders getHeaders() {
        return httpHeaders;
    }

    @Override
    public BaseHttpRequestBuilder addHeader(String key, String value) {
        this.httpHeaders.add(key, value);
        return this;
    }

    public BaseHttpRequestBuilder setBody(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        return this;
    }

    public ByteBuffer getBody() {
        return byteBuffer;
    }

    @Override
    public BaseHttpRequestBuilder setBaseURL(URL baseURL) {
        this.baseURL = baseURL;
        return this;
    }

    public BaseHttpRequestBuilder setBaseURL(HttpAddress httpAddress, String uri, String hostAndPort) {
        Objects.requireNonNull(httpAddress);
        Objects.requireNonNull(uri);
        String scheme = httpAddress.isSecure() ? "https" : "http";
        setAddress(httpAddress);
        setRequestURI(uri);
        String host = stripPort(hostAndPort);
        String extractedPort = extractPort(hostAndPort);
        Integer port = extractedPort != null ? Integer.parseInt(extractedPort) : httpAddress.getPort();
        this.serverURL = URL.builder()
                .scheme(scheme)
                .host(host)
                .port(port)
                .build();
        URLBuilder builder = URL.from(uri).mutator();
        URL url = builder.build();
        if (!url.isAbsolute()) {
            this.baseURL = builder
                    .scheme(scheme)
                    .host(host)
                    .port(port)
                    .build();
        } else {
            this.baseURL = url;
        }
        if (!httpAddress.base().getScheme().equals(url.getScheme())) {
            throw new IllegalArgumentException("scheme mismatch in request: " + httpAddress.base().getScheme() + " != " + url.getScheme());
        }
        if (url.getPort() != null && !httpAddress.getPort().equals(url.getPort())) {
            throw new IllegalArgumentException("port mismatch in request: " + httpAddress.getPort() + " != " + url.getPort());
        }
        return this;
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public BaseHttpRequestBuilder setRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    public String getRequestPath() {
        return requestPath;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return byteBuffer != null ? charset.decode(byteBuffer) : null;
    }

    public BaseHttpRequestBuilder setAddress(HttpAddress httpAddress) {
        this.httpAddress = httpAddress;
        return this;
    }

    public BaseHttpRequestBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public BaseHttpRequestBuilder setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public BaseHttpRequestBuilder setSequenceId(Integer sequenceId) {
        this.sequenceId = sequenceId;
        return this;
    }

    public BaseHttpRequestBuilder setStreamId(Integer streamId) {
        this.streamId = streamId;
        return this;
    }

    public BaseHttpRequestBuilder setRequestId(Long requestId) {
        this.requestId = requestId;
        return this;
    }

    public BaseHttpRequestBuilder setParameter(Parameter parameter) {
        this.parameter = parameter;
        return this;
    }

    private static String stripPort(String hostMaybePort) {
        if (hostMaybePort == null) {
            return null;
        }
        int i = hostMaybePort.lastIndexOf(':');
        return i >= 0 ? hostMaybePort.substring(0, i) : hostMaybePort;
    }

    private static String extractPort(String hostMaybePort) {
        if (hostMaybePort == null) {
            return null;
        }
        int i = hostMaybePort.lastIndexOf(':');
        return i >= 0 ? hostMaybePort.substring(i + 1) : null;
    }

}
