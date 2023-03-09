package org.xbib.net.http.server.netty;

import org.xbib.net.http.server.BaseHttpResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpResponse extends BaseHttpResponse {

    private static final Logger logger = Logger.getLogger(HttpResponse.class.getName());

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
        builder.close();
    }

    @Override
    public void flush() throws IOException {
        builder.flush();
    }
}
