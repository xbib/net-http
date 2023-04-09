package org.xbib.net.http.server.netty.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.IdleTimeoutHandler;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;

public class Http2ChildChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyHttpServer nettyHttpServer;

    private final HttpAddress httpAddress;

    public Http2ChildChannelInitializer(NettyHttpServer nettyHttpServer,
                                        HttpAddress httpAddress) {
        this.nettyHttpServer = nettyHttpServer;
        this.httpAddress = httpAddress;
    }

    @Override
    protected void initChannel(Channel channel) {
        NettyHttpServerConfig nettyHttpServerConfig = nettyHttpServer.getNettyHttpServerConfig();
        channel.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_ADDRESS).set(httpAddress);
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("server-frame-converter",
                new Http2StreamFrameToHttpObjectCodec(true));
        if (nettyHttpServerConfig.isCompressionEnabled()) {
            pipeline.addLast("server-compressor", new HttpContentCompressor());
        }
        if (nettyHttpServerConfig.isDecompressionEnabled()) {
            pipeline.addLast("server-decompressor", new HttpContentDecompressor());
        }
        pipeline.addLast("server-object-aggregator",
                new HttpObjectAggregator(nettyHttpServerConfig.getMaxContentLength()));
        pipeline.addLast("server-chunked-write", new ChunkedWriteHandler());
        pipeline.addLast("server-request", new Http2Handler(nettyHttpServer));
        pipeline.addLast("server-messages", new Http2Messages());
        pipeline.addLast("server-idle-timeout", new IdleTimeoutHandler(nettyHttpServerConfig.getTimeoutMillis()));
    }
}
