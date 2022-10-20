package org.xbib.net.http.server.simple;

import org.xbib.net.http.server.BaseHttpResponse;

import java.io.IOException;

public class HttpResponse extends BaseHttpResponse {

    private final HttpResponseBuilder builder;

    protected HttpResponse(HttpResponseBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    @Override
    public void close() throws IOException {
        builder.internalClose();
    }

    @Override
    public void flush() throws IOException {
        builder.internalFlush();
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

}
