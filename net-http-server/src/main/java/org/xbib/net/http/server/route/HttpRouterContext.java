package org.xbib.net.http.server.route;

import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.xbib.net.Attributes;
import org.xbib.net.URL;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.buffer.DataBufferFactory;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.application.Application;

public interface HttpRouterContext {

    Application getApplication();

    void addOpenHandler(HttpHandler handler);

    List<HttpHandler> getOpenHandlers();

    void addCloseHandler(HttpHandler handler);

    List<HttpHandler> getCloseHandlers();

    HttpRequestBuilder getRequestBuilder();

    void setRequest(HttpRequest httpRequest);

    HttpRequest getRequest();

    //HttpResponseBuilder getResponseBuilder();

    Attributes getAttributes();

    void done();

    boolean isDone();

    void reset();

    void fail();

    boolean isFailed();

    void next();

    boolean isNext();

    void setContextPath(String contextPath);

    String getContextPath();

    void setContextURL(URL url);

    URL getContextURL();

    Path resolve(String path);

    DataBufferFactory getDataBufferFactory();

    HttpRouterContext status(int statusCode);

    HttpRouterContext status(HttpResponseStatus httpResponseStatus);

    HttpResponseStatus status();

    HttpRouterContext charset(Charset charset);

    HttpRouterContext header(String name, String value);

    HttpRouterContext cookie(Cookie cookie);

    HttpRouterContext body(String string) throws IOException;

    HttpRouterContext body(CharBuffer charBuffer, Charset charset) throws IOException;

    HttpRouterContext body(DataBuffer dataBuffer) throws IOException;

    HttpRouterContext body(InputStream inputStream, int bufferSize) throws IOException;

    HttpRouterContext body(FileChannel fileChannel, int bufferSize) throws IOException;

    long lengthInBytes();

    void flush() throws IOException;

    void close() throws IOException;
}
