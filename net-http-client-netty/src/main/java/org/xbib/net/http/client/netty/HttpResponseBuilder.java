package org.xbib.net.http.client.netty;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.CookieBox;

public class HttpResponseBuilder {

    SocketAddress localAddress;

    SocketAddress remoteAddress;

    HttpAddress httpAddress;

    HttpResponseStatus httpStatus;

    HttpHeaders httpHeaders;

    CookieBox cookieBox;

    ByteBuffer byteBuffer;

    CharBuffer charBuffer;

    InputStream inputStream;

    protected HttpResponseBuilder() {
    }

    public HttpResponseBuilder setLocalAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public HttpResponseBuilder setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public HttpResponseBuilder setHttpAddress(HttpAddress httpAddress) {
        this.httpAddress = httpAddress;
        return this;
    }

    public HttpResponseBuilder setStatus(HttpResponseStatus httpResponseStatus) {
        this.httpStatus = httpResponseStatus;
        return this;
    }

    public HttpResponseBuilder setCookieBox(CookieBox cookieBox) {
        this.cookieBox = cookieBox;
        return this;
    }

    public HttpResponseBuilder setHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }

    public HttpResponseBuilder setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        return this;
    }

    public HttpResponse build() {
        return new HttpResponse(this);
    }
}
