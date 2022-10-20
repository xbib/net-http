package org.xbib.net.http.client.netty.secure.http2;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.client.netty.HttpResponseBuilder;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.http2.Http2ChildChannelInitializer;
import org.xbib.net.http.client.netty.http2.Http2Interaction;
import org.xbib.net.http.client.netty.secure.HttpsResponse;
import org.xbib.net.http.client.netty.secure.HttpsResponseBuilder;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

public class Https2Interaction extends Http2Interaction {

    public Https2Interaction(NettyHttpClient nettyHttpClient, HttpAddress httpAddress) {
        super(nettyHttpClient, httpAddress);
    }

    @Override
    protected Http2ChildChannelInitializer newHttp2ChildChannelInitializer(NettyHttpClientConfig clientConfig,
                                                                           Http2Interaction interaction,
                                                                           Channel parentChannel) {
        return new Https2ChildChannelInitializer(clientConfig, interaction, parentChannel);
    }

    @Override
    protected HttpResponseBuilder newHttpResponseBuilder(Channel channel) {
        SslHandler sslHandler = channel.attr(NettyHttpsClientConfig.ATTRIBUTE_KEY_SSL_HANDLER).get();
        HttpsResponseBuilder builder = HttpsResponse.builder();
        if (sslHandler != null) {
            builder.setSSLSession(sslHandler.engine().getSession());
        }
        return builder;
    }
}
