package org.xbib.net.http.server.netty.secure;

import org.xbib.net.http.server.netty.HttpRequest;

import javax.net.ssl.SSLSession;

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

    public String getSNIHost() {
        return builder.sniHost;
    }
}
