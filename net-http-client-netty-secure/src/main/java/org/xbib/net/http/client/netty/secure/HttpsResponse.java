package org.xbib.net.http.client.netty.secure;

import javax.net.ssl.SSLSession;
import org.xbib.net.http.client.netty.HttpResponse;

public class HttpsResponse extends HttpResponse {

    private final HttpsResponseBuilder builder;

    protected HttpsResponse(HttpsResponseBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static HttpsResponseBuilder builder() {
        return new HttpsResponseBuilder();
    }

    public SSLSession getSSLSession() {
        return builder.sslSession;
    }
}
