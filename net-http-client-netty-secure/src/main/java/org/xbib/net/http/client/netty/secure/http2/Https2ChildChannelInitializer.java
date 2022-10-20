package org.xbib.net.http.client.netty.secure.http2;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import org.xbib.net.http.client.netty.Interaction;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.http2.Http2ChildChannelInitializer;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

public class Https2ChildChannelInitializer extends Http2ChildChannelInitializer {

    public Https2ChildChannelInitializer(NettyHttpClientConfig clientConfig, Interaction interaction, Channel parentChannel) {
        super(clientConfig, interaction, parentChannel);
    }

    /**
     * Initialize child channel for HTTP/2, copy the SSL handler attribute so it can be found in interactions.
     *
     * @param ch the {@link Channel} which was registered.
     */
    @Override
    protected void initChannel(Channel ch) {
        super.initChannel(ch);
        SslHandler sslHandler = parentChannel.attr(NettyHttpsClientConfig.ATTRIBUTE_KEY_SSL_HANDLER).get();
        if (sslHandler != null) {
            ch.attr(NettyHttpsClientConfig.ATTRIBUTE_KEY_SSL_HANDLER).set(sslHandler);
        }
    }
}
