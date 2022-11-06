package org.xbib.net.http.client.netty.secure;

import javax.net.ssl.SSLSession;
import org.xbib.net.http.client.netty.HttpResponseBuilder;

public class HttpsResponseBuilder extends HttpResponseBuilder {

    SSLSession sslSession;

    public HttpsResponseBuilder() {
    }

    public HttpsResponseBuilder setSSLSession(SSLSession sslSession) {
        this.sslSession = sslSession;
        return this;
    }

    @Override
    public HttpsResponse build() {
        super.build();
        return new HttpsResponse(this);
    }
}
