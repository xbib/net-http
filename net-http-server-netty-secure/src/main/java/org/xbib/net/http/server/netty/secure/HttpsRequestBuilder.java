package org.xbib.net.http.server.netty.secure;

import io.netty.handler.codec.http.FullHttpRequest;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.HttpRequestBuilder;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;

public class HttpsRequestBuilder extends HttpRequestBuilder {

    protected SSLSession sslSession;

    protected String sniHost;

    protected HttpsRequestBuilder() {
    }

    public HttpsRequestBuilder setFullHttpRequest(FullHttpRequest fullHttpRequest) {
        super.setFullHttpRequest(fullHttpRequest);
        return this;
    }

    @Override
    public HttpsRequestBuilder setAddress(HttpAddress httpAddress) {
        super.setAddress(httpAddress);
        return this;
    }

    @Override
    public HttpsRequestBuilder setLocalAddress(InetSocketAddress localAddress) {
        super.setLocalAddress(localAddress);
        return this;
    }

    @Override
    public HttpsRequestBuilder setRemoteAddress(InetSocketAddress remoteAddress) {
        super.setRemoteAddress(remoteAddress);
        return this;
    }

    @Override
    public HttpsRequestBuilder setBaseURL(URL baseURL) {
        super.setBaseURL(baseURL);
        return this;
    }

    @Override
    public HttpsRequestBuilder setBaseURL(HttpAddress httpAddress, String uri, String hostAndPort) {
        super.setBaseURL(httpAddress, uri, hostAndPort);
        return this;
    }

    @Override
    public HttpsRequestBuilder setSequenceId(Integer sequenceId) {
        super.setSequenceId(sequenceId);
        return this;
    }

    @Override
    public HttpsRequestBuilder setStreamId(Integer streamId) {
        super.setStreamId(streamId);
        return this;
    }

    @Override
    public HttpsRequestBuilder setRequestId(Long requestId) {
        super.setRequestId(requestId);
        return this;
    }

    public HttpsRequestBuilder setSNIHost(String host) {
        this.sniHost = host;
        return this;
    }

    public HttpsRequestBuilder setSSLSession(SSLSession sslSession) {
        this.sslSession = sslSession;
        return this;
    }

    public HttpsRequest build() {
        return new HttpsRequest(this);
    }
}
