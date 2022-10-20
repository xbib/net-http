package org.xbib.net.http.client.netty.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import org.xbib.net.http.client.netty.Interaction;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;

public class Http2ChildChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyHttpClientConfig clientConfig;

    private final Interaction interaction;

    protected final Channel parentChannel;

    public Http2ChildChannelInitializer(NettyHttpClientConfig clientConfig, Interaction interaction, Channel parentChannel) {
        this.clientConfig = clientConfig;
        this.interaction = interaction;
        this.parentChannel = parentChannel;
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast("child-client-frame-converter",
                new Http2StreamFrameToHttpObjectCodec(false));
        p.addLast("child-client-decompressor",
                new HttpContentDecompressor());
        p.addLast("child-client-chunk-aggregator",
                new HttpObjectAggregator(clientConfig.getMaxContentLength()));
        p.addLast("child-client-response-handler",
                new Http2Handler(interaction));
    }
}
