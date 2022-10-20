package org.xbib.net.http.server.netty.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.HttpChannelInitializer;
import org.xbib.net.http.server.netty.IdleTimeoutHandler;
import org.xbib.net.http.server.netty.NettyCustomizer;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.netty.TrafficLoggingHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Insecure HTTP/2 server channel initializer.
 *
 * This initializer uses a "h2c" cleartext channel, which is not supported by most of the web browsers.
 */
public class Http2ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Http2ChannelInitializer.class.getName());

    public Http2ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress address) {
        return org.xbib.net.http.HttpVersion.HTTP_2_0.equals(address.getVersion()) && !address.isSecure();
    }

    @Override
    public void init(Channel channel, NettyHttpServer nettyHttpServer, NettyCustomizer customizer) {
        final HttpAddress httpAddress = channel.attr(NettyHttpServerConfig.ATTRIBUTE_KEY_HTTP_ADDRESS).get();
        final NettyHttpServerConfig nettyHttpServerConfig = nettyHttpServer.getNettyHttpServerConfig();
        ChannelPipeline pipeline = channel.pipeline();
        if (nettyHttpServerConfig.isDebug()) {
            pipeline.addLast("server-logging", new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        pipeline.addLast("server-upgrade", createUpgradeHandler(nettyHttpServer, httpAddress));
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
        if (customizer != null) {
            customizer.afterChannelInitialized(channel);
        }
        if (nettyHttpServerConfig.isDebug() && logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "HTTP/2 cleartext server channel initialized: address = " +
                    channel.localAddress() + " pipeline = " + pipeline.names());
        }
    }

    /**
     * This upgrade handler ensures to upgrade to HTTP 2.0 via prior knowledge.
     * @param nettyHttpServer the netty server
     * @param httpAddress the HTTP address
     * @return the CleartextHttp2ServerUpgradeHandler
     */
    protected CleartextHttp2ServerUpgradeHandler createUpgradeHandler(NettyHttpServer nettyHttpServer,
                                                                      HttpAddress httpAddress) {
        NettyHttpServerConfig nettyHttpServerConfig = nettyHttpServer.getNettyHttpServerConfig();
        Http2ChildChannelInitializer childHandler =
                new Http2ChildChannelInitializer(nettyHttpServer, httpAddress);
        Http2MultiplexCodecBuilder multiplexCodecBuilder = Http2MultiplexCodecBuilder.forServer(childHandler)
                .initialSettings(Http2Settings.defaultSettings());
        if (nettyHttpServerConfig.isDebug()) {
            multiplexCodecBuilder.frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "server"));
        }
        Http2MultiplexCodec multiplexCodec = multiplexCodecBuilder.build();
        HttpServerCodec serverCodec = new HttpServerCodec();
        HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(serverCodec, protocol -> {
            logger.log(Level.INFO, "upgrade handler protocol = " + protocol);
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(multiplexCodec);
            } else {
                return null;
            }
        });
        return new CleartextHttp2ServerUpgradeHandler(serverCodec, upgradeHandler, multiplexCodec);
    }
}
