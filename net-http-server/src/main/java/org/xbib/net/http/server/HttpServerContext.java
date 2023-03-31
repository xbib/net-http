package org.xbib.net.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.xbib.net.Attributes;
import org.xbib.net.URL;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.route.HttpRouteResolver;
import org.xbib.net.http.server.service.HttpService;

public interface HttpServerContext {

    Application getApplication();

    HttpRequestBuilder request();

    HttpResponseBuilder response();

    void setResolverResult(HttpRouteResolver.Result<HttpService> result);

    Attributes getAttributes();

    void done();

    boolean isDone();

    void fail();

    boolean isFailed();

    void next();

    boolean isNext();

    HttpRequest httpRequest();

    String getContextPath();

    URL getContextURL();

    Path resolve(String path);

    void write() throws IOException;

    void write(String string) throws IOException;

    void write(CharBuffer charBuffer, Charset charset) throws IOException;

    void write(DataBuffer dataBuffer) throws IOException;

    void write(InputStream inputStream, int bufferSize) throws IOException;

    void write(FileChannel fileChannel, int bufferSize) throws IOException;
}
