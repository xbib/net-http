package org.xbib.net.http.client.netty.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.xbib.net.http.client.netty.Interaction;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;

public class Http2ChildChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyHttpClientConfig nettyHttpClientConfig;

    private final Interaction interaction;

    protected final Channel parentChannel;

    public Http2ChildChannelInitializer(NettyHttpClientConfig nettyHttpClientConfig, Interaction interaction, Channel parentChannel) {
        this.nettyHttpClientConfig = nettyHttpClientConfig;
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
        if (nettyHttpClientConfig.isChunkWriteEnabled()) {
            p.addLast("child-chunk-write", new ChunkedWriteHandler());
        }
        if (nettyHttpClientConfig.isObjectAggregationEnabled()) {
            p.addLast("child-client-object-aggregator",
                    new HttpObjectAggregator(nettyHttpClientConfig.getMaxContentLength()));
        }
        p.addLast("child-client-response-handler",
                new Http2Handler(interaction));
    }
}
