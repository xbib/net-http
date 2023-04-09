package org.xbib.net.http.server.netty.secure.http1;

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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.HttpChannelInitializer;
import org.xbib.net.http.server.netty.NettyCustomizer;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.netty.http1.HttpFileUploadHandler;
import org.xbib.net.http.server.netty.http1.HttpPipeliningHandler;
import org.xbib.net.http.server.netty.IdleTimeoutHandler;
import org.xbib.net.http.server.netty.TrafficLoggingHandler;
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.netty.secure.ServerNameIndicationHandler;
import org.xbib.net.http.server.netty.secure.http2.Https2ChildChannelInitializer;

public class Https1ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Https1ChannelInitializer.class.getName());

    public Https1ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress address) {
        return org.xbib.net.http.HttpVersion.HTTP_1_1.equals(address.getVersion()) && address.isSecure();
    }

    @Override
    public void init(Channel channel, NettyHttpServer nettyHttpServer, NettyCustomizer customizer) {
        final HttpAddress httpAddress = channel.attr(NettyHttpsServerConfig.ATTRIBUTE_HTTP_ADDRESS).get();
        final NettyHttpsServerConfig nettyHttpsServerConfig = (NettyHttpsServerConfig) nettyHttpServer.getNettyHttpServerConfig();
        final ServerNameIndicationHandler serverNameIndicationHandler =
                new ServerNameIndicationHandler(nettyHttpsServerConfig, httpAddress,
                        nettyHttpsServerConfig.getDomainNameMapping(nettyHttpServer.getDomains()));
        channel.attr(NettyHttpsServerConfig.ATTRIBUTE_KEY_SNI_HANDLER).set(serverNameIndicationHandler);
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("server-sni", serverNameIndicationHandler);
        HttpServerCodec httpServerCodec = new HttpServerCodec(nettyHttpsServerConfig.getMaxInitialLineLength(),
                nettyHttpsServerConfig.getMaxHeadersSize(), nettyHttpsServerConfig.getMaxChunkSize());
        pipeline.addLast("server-codec", httpServerCodec);
        if (nettyHttpsServerConfig.isCompressionEnabled()) {
            pipeline.addLast("server-compressor", new HttpContentCompressor());
        }
        if (nettyHttpsServerConfig.isDecompressionEnabled()) {
            pipeline.addLast("server-decompressor", new HttpContentDecompressor());
        }
        if (nettyHttpsServerConfig.isObjectAggregationEnabled()) {
            HttpObjectAggregator httpObjectAggregator = new HttpObjectAggregator(nettyHttpsServerConfig.getMaxContentLength());
            httpObjectAggregator.setMaxCumulationBufferComponents(nettyHttpsServerConfig.getMaxCompositeBufferComponents());
            pipeline.addLast("server-aggregator", httpObjectAggregator);
        }
        if (nettyHttpsServerConfig.isFileUploadEnabled()) {
            HttpFileUploadHandler httpFileUploadHandler = new HttpFileUploadHandler(nettyHttpServer);
            pipeline.addLast("server-file-upload", httpFileUploadHandler);
        }
        if (nettyHttpsServerConfig.isChunkedWriteEnabled()) {
            pipeline.addLast("server-chunked-write", new ChunkedWriteHandler());
        }
        if (nettyHttpsServerConfig.isPipeliningEnabled()) {
            pipeline.addLast("server-pipelining", new HttpPipeliningHandler(nettyHttpsServerConfig.getPipeliningCapacity()));
        }
        pipeline.addLast("server-messages", new Https1Handler(nettyHttpServer));
        pipeline.addLast("server-idle-timeout", new IdleTimeoutHandler(nettyHttpsServerConfig.getTimeoutMillis()));
        if (nettyHttpsServerConfig.isDebug()) {
            pipeline.addLast("server-logging", new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        if (customizer != null) {
            customizer.afterChannelInitialized(channel);
        }
        if (nettyHttpsServerConfig.isDebug()) {
            logger.log(Level.FINE, "HTTP/1.1 secure server channel initialized: address=" + channel.localAddress() +
                    " pipeline=" + pipeline.names());
        }
    }

    /**
     * This upgrade handler ensures to upgrade to HTTPS 2.0 via prior knowledge.
     * @param nettyHttpServer the netty server
     * @param httpAddress the HTTP address
     * @param serverNameIndicationHandler the SNI handler
     * @return the CleartextHttp2ServerUpgradeHandler
     */
    protected CleartextHttp2ServerUpgradeHandler createUpgradeHandler(NettyHttpServer nettyHttpServer,
                                                                      HttpAddress httpAddress,
                                                                      ServerNameIndicationHandler serverNameIndicationHandler) {
        NettyHttpServerConfig nettyHttpServerConfig = nettyHttpServer.getNettyHttpServerConfig();
        Https2ChildChannelInitializer childHandler =
                new Https2ChildChannelInitializer(nettyHttpServer, httpAddress, serverNameIndicationHandler);
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
