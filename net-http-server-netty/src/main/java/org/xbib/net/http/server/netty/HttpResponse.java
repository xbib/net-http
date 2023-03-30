package org.xbib.net.http.server.netty;

import org.xbib.net.http.server.BaseHttpResponse;

import java.io.IOException;

public class HttpResponse extends BaseHttpResponse {

    private final HttpResponseBuilder builder;

    protected HttpResponse(HttpResponseBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    @Override
    public void close() throws IOException {
        builder.release();
    }

    @Override
    public void flush() throws IOException {
        builder.flush();
    }
}
