package org.xbib.net.http.server.simple;

import org.xbib.net.Request;
import org.xbib.net.http.server.BaseHttpRequest;
import org.xbib.net.util.ByteBufferInputStream;

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
        return new ByteBufferInputStream(builder.getBody());
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
