package org.xbib.net.http.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.BindException;
import java.util.Collection;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.domain.HttpDomain;

public interface HttpServer extends Closeable {

    void bind() throws BindException;

    void loop() throws IOException;

    void dispatch(HttpRequestBuilder requestBuilder,
                  HttpResponseBuilder responseBuilder);

    void dispatch(HttpRequestBuilder requestBuilder,
                  HttpResponseBuilder responseBuilder,
                  HttpResponseStatus responseStatus);

    Collection<HttpDomain> getDomains();

}
