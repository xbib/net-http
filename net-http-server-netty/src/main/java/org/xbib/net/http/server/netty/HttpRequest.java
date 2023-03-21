package org.xbib.net.http.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.xbib.net.Request;
import org.xbib.net.http.server.BaseHttpRequest;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
        return new ByteBufInputStream(builder.fullHttpRequest.content());
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
        return "HttpRequest[request=" + builder.fullHttpRequest +
                ",parameter=" + builder.getParameter() +
                ",body=" + builder.fullHttpRequest.content().toString(StandardCharsets.UTF_8) +
                "]";
    }

    public ByteBuf getByteBuf() {
        return builder.fullHttpRequest.content();
    }
}
