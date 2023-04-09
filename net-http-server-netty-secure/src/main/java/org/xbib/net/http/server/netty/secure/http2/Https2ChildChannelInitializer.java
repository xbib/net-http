package org.xbib.net.http.server.netty.secure.http2;

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
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.netty.secure.ServerNameIndicationHandler;

public class Https2ChildChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyHttpServer server;

    private final HttpAddress httpAddress;

    private final ServerNameIndicationHandler serverNameIndicationHandler;

    public Https2ChildChannelInitializer(NettyHttpServer server,
                                         HttpAddress httpAddress,
                                         ServerNameIndicationHandler serverNameIndicationHandler) {
        this.server = server;
        this.httpAddress = httpAddress;
        this.serverNameIndicationHandler = serverNameIndicationHandler;
    }

    @Override
    protected void initChannel(Channel channel) {
        NettyHttpsServerConfig nettyHttpsServerConfig = (NettyHttpsServerConfig) server.getNettyHttpServerConfig();
        channel.attr(NettyHttpsServerConfig.ATTRIBUTE_HTTP_ADDRESS).set(httpAddress);
        channel.attr(NettyHttpsServerConfig.ATTRIBUTE_KEY_SNI_HANDLER).set(serverNameIndicationHandler);
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("server-frame-converter", new Http2StreamFrameToHttpObjectCodec(true));
        if (nettyHttpsServerConfig.isCompressionEnabled()) {
            pipeline.addLast("server-compressor", new HttpContentCompressor());
        }
        if (nettyHttpsServerConfig.isDecompressionEnabled()) {
            pipeline.addLast("server-decompressor", new HttpContentDecompressor());
        }
        pipeline.addLast("server-object-aggregator", new HttpObjectAggregator(nettyHttpsServerConfig.getMaxContentLength()));
        pipeline.addLast("server-chunked-write", new ChunkedWriteHandler());
        pipeline.addLast("server-request", new Https2Handler(server));
        pipeline.addLast("server-messages", new Https2Messages());
        pipeline.addLast("server-idle-timeout", new IdleTimeoutHandler(nettyHttpsServerConfig.getTimeoutMillis()));
    }
}
