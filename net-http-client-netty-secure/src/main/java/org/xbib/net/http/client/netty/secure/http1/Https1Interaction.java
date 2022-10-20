package org.xbib.net.http.client.netty.secure.http1;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import javax.net.ssl.SSLSession;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.client.netty.HttpResponseBuilder;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.http1.Http1Interaction;
import org.xbib.net.http.client.netty.http2.Http2Interaction;
import org.xbib.net.http.client.netty.secure.HttpsResponse;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;
import org.xbib.net.http.client.netty.secure.http2.Https2Interaction;

public class Https1Interaction extends Http1Interaction {

    public Https1Interaction(NettyHttpClient nettyHttpClient, HttpAddress httpAddress) {
        super(nettyHttpClient, httpAddress);
    }

    @Override
    protected HttpResponseBuilder newHttpResponseBuilder(Channel channel) {
        SslHandler sslHandler = channel.attr(NettyHttpsClientConfig.ATTRIBUTE_KEY_SSL_HANDLER).get();
        SSLSession sslSession = sslHandler != null ? sslHandler.engine().getSession() : null;
        return HttpsResponse.builder().setSSLSession(sslSession);
    }

    @Override
    protected Http2Interaction upgradeInteraction() {
        return new Https2Interaction(nettyHttpClient, httpAddress);
    }
}
