package org.xbib.net.http.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;

public abstract class BaseHttpRequest implements HttpRequest {

    protected final BaseHttpRequestBuilder builder;

    protected BaseHttpRequest(BaseHttpRequestBuilder builder) {
        this.builder = builder;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return builder.localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return builder.remoteAddress;
    }

    @Override
    public URL getBaseURL() {
        return builder.url;
    }

    @Override
    public HttpVersion getVersion() {
        return builder.httpVersion;
    }

    @Override
    public HttpMethod getMethod() {
        return builder.httpMethod;
    }

    @Override
    public HttpHeaders getHeaders() {
        return builder.httpHeaders;
    }

    @Override
    public ParameterBuilder getParameters() {
        return builder.parameterBuilder;
    }

    @Override
    public ByteBuffer getBody() {
        return builder.byteBuffer;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return builder.byteBuffer != null ? charset.decode(builder.byteBuffer) : null;
    }
}
