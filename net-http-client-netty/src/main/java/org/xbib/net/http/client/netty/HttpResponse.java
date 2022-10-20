package org.xbib.net.http.client.netty;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.CookieBox;
import org.xbib.net.util.ByteBufferInputStream;

public class HttpResponse implements org.xbib.net.http.client.HttpResponse, Closeable {

    private final HttpResponseBuilder builder;

    protected HttpResponse(HttpResponseBuilder builder) {
        this.builder = builder;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    public SocketAddress getLocalAddress() {
        return builder.localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return builder.remoteAddress;
    }

    @Override
    public HttpAddress getAddress() {
        return builder.httpAddress;
    }

    @Override
    public HttpResponseStatus getStatus() {
        return builder.httpStatus;
    }

    @Override
    public HttpHeaders getHeaders() {
        return builder.httpHeaders;
    }

    @Override
    public CookieBox getCookies() {
        return builder.cookieBox;
    }

    @Override
    public ByteBuffer getBody() {
        return builder.byteBuffer;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return charset.decode(builder.byteBuffer);
    }

    @Override
    public InputStream getBodyAsStream() {
        return new ByteBufferInputStream(builder.byteBuffer);
    }

    @Override
    public void release() {
        // nothing to do
    }

    @Override
    public void close() throws IOException {
        release();
    }
}
