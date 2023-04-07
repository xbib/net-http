package org.xbib.net.http.client.netty.secure;

import javax.net.ssl.SSLSession;
import org.xbib.net.http.client.netty.HttpRequestBuilder;

public class HttpsRequestBuilder extends HttpRequestBuilder {

    SSLSession sslSession;

    public HttpsRequestBuilder() {
    }

    public HttpsRequestBuilder setSSLSession(SSLSession sslSession) {
        this.sslSession = sslSession;
        return this;
    }

    public HttpsRequest build() {
        this.headers = validateHeaders(headers);
        return new HttpsRequest(this);
    }
}
