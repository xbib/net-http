package org.xbib.net.http.client;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.Request;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.cookie.Cookie;

public interface HttpRequest extends Request {

    URL getURL();

    HttpVersion getVersion();

    HttpMethod getMethod();

    HttpHeaders getHeaders();

    ParameterBuilder getParameterBuilder();

    ByteBuffer getBody();

    CharBuffer getBodyAsChars(Charset charset);

    List<Message> getMessages();

    boolean isBackOffEnabled();

    BackOff getBackOff();

    Collection<Cookie> cookies();

}
