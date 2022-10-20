package org.xbib.net.http.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;

public abstract class BaseHttpRequestBuilder implements HttpRequestBuilder {

    HttpAddress httpAddress;

    InetSocketAddress localAddress;

    InetSocketAddress remoteAddress;

    URL url;

    String requestPath;

    ParameterBuilder parameterBuilder;

    Integer sequenceId;

    Integer streamId;

    Long requestId;

    HttpVersion httpVersion;

    HttpMethod httpMethod;

    HttpHeaders httpHeaders = new HttpHeaders();

    ByteBuffer byteBuffer;

    protected BaseHttpRequestBuilder() {
    }

    public BaseHttpRequestBuilder setVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return this;
    }

    public HttpVersion getVersion() {
        return httpVersion;
    }

    public BaseHttpRequestBuilder setMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public HttpMethod getMethod() {
        return httpMethod;
    }

    public BaseHttpRequestBuilder setHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }

    public BaseHttpRequestBuilder addHeader(String key, String value) {
        this.httpHeaders.add(key, value);
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setAddress(HttpAddress httpAddress) {
        this.httpAddress = httpAddress;
        return this;
    }
    @Override
    public BaseHttpRequestBuilder setURL(URL url) {
        this.url = url;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setParameterBuilder(ParameterBuilder parameterBuilder) {
        this.parameterBuilder = parameterBuilder;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setBody(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
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
}
