package org.xbib.net.http.server.nio;

import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.BaseHttpRequestBuilder;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class HttpRequestBuilder extends BaseHttpRequestBuilder {

    HttpRequestBuilder() {
    }

    @Override
    public HttpRequestBuilder setVersion(HttpVersion httpVersion) {
        super.setVersion(httpVersion);
        return this;
    }

    @Override
    public HttpRequestBuilder setMethod(HttpMethod httpMethod) {
        super.setMethod(httpMethod);
        return this;
    }

    @Override
    public HttpRequestBuilder setRequestURI(String requestURI) {
        super.setRequestURI(requestURI);
        return this;
    }

    @Override
    public HttpRequestBuilder setHeaders(HttpHeaders httpHeaders) {
        super.setHeaders(httpHeaders);
        return this;
    }

    @Override
    public HttpRequestBuilder setBody(ByteBuffer byteBuffer) {
        super.setBody(byteBuffer);
        return this;
    }

    @Override
    public HttpRequestBuilder setLocalAddress(InetSocketAddress localAddress) {
        super.setLocalAddress(localAddress);
        return this;
    }

    @Override
    public HttpRequestBuilder setRemoteAddress(InetSocketAddress remoteAddress) {
        super.setRemoteAddress(remoteAddress);
        return this;
    }

    @Override
    public HttpRequestBuilder setBaseURL(HttpAddress httpAddress, String uri, String hostAndPort) {
        super.setBaseURL(httpAddress, uri, hostAndPort);
        return this;
    }

    @Override
    public HttpRequestBuilder setBaseURL(URL baseURL) {
        super.setBaseURL(baseURL);
        return this;
    }

    @Override
    public HttpRequestBuilder setSequenceId(Integer sequenceId) {
        super.setSequenceId(sequenceId);
        return this;
    }

    @Override
    public HttpRequestBuilder setStreamId(Integer streamId) {
        super.setStreamId(streamId);
        return this;
    }

    @Override
    public HttpRequestBuilder setRequestId(Long requestId) {
        super.setRequestId(requestId);
        return this;
    }

    @Override
    public HttpRequest build() {
        return new HttpRequest(this);
    }

    public InputStream getInputStream() {
        return null;
    }
}
