package org.xbib.net.http.server.nio;

import org.xbib.net.Request;
import org.xbib.net.http.server.BaseHttpRequest;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class HttpRequest extends BaseHttpRequest {

    private final HttpRequestBuilder builder;

    protected HttpRequest(HttpRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    @Override
    public InputStream getInputStream() {
        return builder.getInputStream();
    }

    @Override
    public ByteBuffer getBody() {
        return builder.getBody();
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return builder.getBodyAsChars(charset);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Request> R as(Class<R> cl) {
        return (R) this;
    }

    public static HttpRequestBuilder builder() {
        return new HttpRequestBuilder();
    }
}
