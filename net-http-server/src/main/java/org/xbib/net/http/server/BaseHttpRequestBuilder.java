package org.xbib.net.http.server;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.xbib.net.Parameter;
import org.xbib.net.URL;
import org.xbib.net.URLBuilder;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;

public abstract class BaseHttpRequestBuilder implements HttpRequestBuilder {

    protected HttpServerContext httpServerContext;

    protected HttpAddress httpAddress;

    protected InetSocketAddress localAddress;

    protected InetSocketAddress remoteAddress;

    protected URL serverURL;

    protected URL baseURL;

    protected String requestPath;

    protected Parameter parameter;

    protected Integer sequenceId;

    protected Integer streamId;

    protected Long requestId;

    protected HttpVersion httpVersion;

    protected HttpMethod httpMethod;

    protected HttpHeaders httpHeaders;

    protected String requestURI;

    protected ByteBuffer byteBuffer;

    protected boolean done;

    protected List<Message> messages;

    protected BaseHttpRequestBuilder() {
        this.httpHeaders = new HttpHeaders();
        this.messages = new ArrayList<>();
    }

    @Override
    public BaseHttpRequestBuilder setContext(HttpServerContext httpServerContext) {
        if (done) {
            return this;
        }
        this.httpServerContext = httpServerContext;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setVersion(HttpVersion httpVersion) {
        if (done) {
            return this;
        }
        this.httpVersion = httpVersion;
        return this;
    }

    public HttpVersion getVersion() {
        return httpVersion;
    }

    @Override
    public BaseHttpRequestBuilder setMethod(HttpMethod httpMethod) {
        if (done) {
            return this;
        }
        this.httpMethod = httpMethod;
        return this;
    }

    public HttpMethod getMethod() {
        return httpMethod;
    }

    @Override
    public BaseHttpRequestBuilder setRequestURI(String requestURI) {
        if (done) {
            return this;
        }
        this.requestURI = requestURI;
        return this;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public BaseHttpRequestBuilder setHeaders(HttpHeaders httpHeaders) {
        if (done) {
            return this;
        }
        this.httpHeaders = httpHeaders;
        return this;
    }

    @Override
    public HttpHeaders getHeaders() {
        return httpHeaders;
    }

    @Override
    public BaseHttpRequestBuilder addHeader(String key, String value) {
        if (done) {
            return this;
        }
        this.httpHeaders.add(key, value);
        return this;
    }

    public BaseHttpRequestBuilder setBody(ByteBuffer byteBuffer) {
        if (done) {
            return this;
        }
        this.byteBuffer = byteBuffer;
        return this;
    }

    public ByteBuffer getBody() {
        return byteBuffer;
    }

    @Override
    public BaseHttpRequestBuilder setBaseURL(URL baseURL) {
        if (done) {
            return this;
        }
        this.baseURL = baseURL;
        return this;
    }

    public BaseHttpRequestBuilder setBaseURL(HttpAddress httpAddress, String uri, String hostAndPort) {
        if (done) {
            return this;
        }
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

    @Override
    public URL getBaseURL() {
        return baseURL;
    }

    @Override
    public BaseHttpRequestBuilder setRequestPath(String requestPath) {
        if (done) {
            return this;
        }
        this.requestPath = requestPath;
        return this;
    }

    @Override
    public String getRequestPath() {
        return requestPath;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return byteBuffer != null ? charset.decode(byteBuffer) : null;
    }

    @Override
    public BaseHttpRequestBuilder setParameter(Parameter parameter) {
        if (done) {
            return this;
        }
        this.parameter = parameter;
        return this;
    }

    public BaseHttpRequestBuilder setAddress(HttpAddress httpAddress) {
        if (done) {
            return this;
        }
        this.httpAddress = httpAddress;
        return this;
    }

    public BaseHttpRequestBuilder setLocalAddress(InetSocketAddress localAddress) {
        if (done) {
            return this;
        }
        this.localAddress = localAddress;
        return this;
    }

    public BaseHttpRequestBuilder setRemoteAddress(InetSocketAddress remoteAddress) {
        if (done) {
            return this;
        }
        this.remoteAddress = remoteAddress;
        return this;
    }

    public BaseHttpRequestBuilder setSequenceId(Integer sequenceId) {
        if (done) {
            return this;
        }
        this.sequenceId = sequenceId;
        return this;
    }

    public BaseHttpRequestBuilder setStreamId(Integer streamId) {
        if (done) {
            return this;
        }
        this.streamId = streamId;
        return this;
    }

    public BaseHttpRequestBuilder setRequestId(Long requestId) {
        if (done) {
            return this;
        }
        this.requestId = requestId;
        return this;
    }

    public BaseHttpRequestBuilder addPart(Message message) {
        if (done) {
            return this;
        }
        this.messages.add(message);
        return this;
    }

    @Override
    public void done() {
        this.done = true;
    }

    @Override
    public void release() {
        // do nothing for now
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
