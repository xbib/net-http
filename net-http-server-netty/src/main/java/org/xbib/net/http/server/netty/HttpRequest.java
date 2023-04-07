package org.xbib.net.http.server.netty;

import org.xbib.net.Request;
import org.xbib.net.http.server.BaseHttpRequest;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Objects;
import org.xbib.net.util.ByteBufferInputStream;

public class HttpRequest extends BaseHttpRequest {

    private final HttpRequestBuilder builder;

    protected HttpRequest(HttpRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static HttpRequestBuilder builder() {
        return new HttpRequestBuilder();
    }

    @Override
    public InputStream getInputStream() {
        //return new ByteBufInputStream(builder.fullHttpRequest.content());
        return builder.byteBuffer != null ? new ByteBufferInputStream(builder.byteBuffer) : null;
    }

    @Override
    public ByteBuffer getBody() {
        return builder.getBody();
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return builder.getBodyAsChars(charset);
    }

    @Override
    public <R extends Request> R as(Class<R> type) {
        Objects.requireNonNull(type);
        return type.isInstance(this) ? type.cast(this) :  null;
    }

    @Override
    public String toString() {
        return "HttpRequest[method=" + builder.getMethod() +
                ",version=" + builder.getVersion() +
                ",parameter=" + builder.getParameter() +
                ",body=" + (builder.byteBuffer != null) +
                "]";
    }
}
