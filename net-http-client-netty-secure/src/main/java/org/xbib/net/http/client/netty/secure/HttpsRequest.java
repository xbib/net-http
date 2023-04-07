package org.xbib.net.http.client.netty.secure;

import javax.net.ssl.SSLSession;
import org.xbib.net.http.client.netty.HttpRequest;

public class HttpsRequest extends HttpRequest {

    private final HttpsRequestBuilder builder;

    protected HttpsRequest(HttpsRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static HttpsRequestBuilder builder() {
        return new HttpsRequestBuilder();
    }

    public SSLSession getSSLSession() {
        return builder.sslSession;
    }
}
