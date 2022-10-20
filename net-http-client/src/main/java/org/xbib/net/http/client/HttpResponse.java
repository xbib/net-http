package org.xbib.net.http.client;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.CookieBox;

public interface HttpResponse {

    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();

    HttpAddress getAddress();

    HttpResponseStatus getStatus();

    HttpHeaders getHeaders();

    CookieBox getCookies();

    ByteBuffer getBody();

    CharBuffer getBodyAsChars(Charset charset);

    InputStream getBodyAsStream();

    void release();
}
