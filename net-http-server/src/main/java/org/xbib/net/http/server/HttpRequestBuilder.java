package org.xbib.net.http.server;

import org.xbib.net.Parameter;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;

import java.io.Closeable;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public interface HttpRequestBuilder extends Closeable {

    HttpRequestBuilder setAddress(HttpAddress httpAddress);

    HttpRequestBuilder setBaseURL(URL baseURL);

    HttpRequestBuilder setRequestURI(String requestURI);

    HttpRequestBuilder setRequestPath(String requestPath);

    HttpRequestBuilder setParameter(Parameter parameter);

    HttpRequestBuilder setContext(HttpServerContext context);

    HttpRequestBuilder setVersion(HttpVersion version);

    HttpRequestBuilder setMethod(HttpMethod method);

    HttpRequestBuilder setHeaders(HttpHeaders httpHeaders);

    HttpRequestBuilder addHeader(String name, String value);

    URL getBaseURL();

    HttpMethod getMethod();

    String getRequestURI();

    String getRequestPath();

    HttpHeaders getHeaders();

    CharBuffer getBodyAsChars(Charset charset);

    HttpRequest build();

    void done();

}
