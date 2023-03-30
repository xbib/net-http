package org.xbib.net.http.server.netty;

import org.xbib.net.http.server.BaseHttpResponse;

import java.io.IOException;

public class HttpResponse extends BaseHttpResponse {

    protected HttpResponse(HttpResponseBuilder builder) {
        super(builder);
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    @Override
    public void flush() throws IOException {
        // ignore
    }
}
