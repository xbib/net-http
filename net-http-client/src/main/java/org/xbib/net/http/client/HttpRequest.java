package org.xbib.net.http.client;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.Request;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;

public interface HttpRequest extends Request {

    URL getURL();

    HttpVersion getVersion();

    HttpMethod getMethod();

    HttpHeaders getHeaders();

    ParameterBuilder getParameters();

    ByteBuffer getBody();

    CharBuffer getBodyAsChars(Charset charset);

}
