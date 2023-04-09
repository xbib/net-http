package org.xbib.net.http.server.netty.secure.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.HttpChannelInitializer;
import org.xbib.net.http.server.netty.IdleTimeoutHandler;
import org.xbib.net.http.server.netty.NettyCustomizer;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.netty.TrafficLoggingHandler;
import org.xbib.net.http.server.netty.http1.HttpFileUploadHandler;
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.netty.secure.ServerNameIndicationHandler;

public class Https2ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Https2ChannelInitializer.class.getName());

    public Https2ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress address) {
        return org.xbib.net.http.HttpVersion.HTTP_2_0.equals(address.getVersion()) && address.isSecure();
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
        if (nettyHttpsServerConfig.isDebug()) {
            pipeline.addLast("server-logger", new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        pipeline.addLast("server-upgrade", createUpgradeHandler(nettyHttpServer, httpAddress, serverNameIndicationHandler));
        if (nettyHttpsServerConfig.isObjectAggregationEnabled()) {
            pipeline.addLast("server-object-aggregator", new HttpObjectAggregator(nettyHttpsServerConfig.getMaxContentLength()));
        }
        if (nettyHttpsServerConfig.isFileUploadEnabled()) {
            HttpFileUploadHandler httpFileUploadHandler = new HttpFileUploadHandler(nettyHttpServer);
            pipeline.addLast("server-file-upload", httpFileUploadHandler);
        }
        if (nettyHttpsServerConfig.isChunkedWriteEnabled()) {
            pipeline.addLast("server-chunked-write", new ChunkedWriteHandler());
        }
        pipeline.addLast("server-requests", new Https2Handler(nettyHttpServer));
        pipeline.addLast("server-messages", new Https2Messages());
        pipeline.addLast("server-idle-timeout", new IdleTimeoutHandler(nettyHttpsServerConfig.getTimeoutMillis()));
        if (customizer != null) {
            customizer.afterChannelInitialized(channel);
        }
        if (nettyHttpsServerConfig.isDebug()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "HTTP/2 secure server channel initialized: address=" +
                        channel.localAddress() + " pipeline=" + channel.pipeline().names());
            }
        }
    }

    protected CleartextHttp2ServerUpgradeHandler createUpgradeHandler(NettyHttpServer nettyHttpServer,
                                                                      HttpAddress httpAddress,
                                                                      ServerNameIndicationHandler serverNameIndicationHandler) {
        NettyHttpServerConfig nettyHttpServerConfig = nettyHttpServer.getNettyHttpServerConfig();
        Https2ChildChannelInitializer childHandler =
                new Https2ChildChannelInitializer(nettyHttpServer, httpAddress, serverNameIndicationHandler);
        // TODO replace Http2MultiplexCodecBuilder
        Http2MultiplexCodecBuilder multiplexCodecBuilder = Http2MultiplexCodecBuilder.forServer(childHandler)
                .initialSettings(Http2Settings.defaultSettings());
        if (nettyHttpServerConfig.isDebug()) {
            multiplexCodecBuilder.frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "server"));
        }
        Http2MultiplexCodec multiplexCodec = multiplexCodecBuilder.build();
        HttpServerCodec serverCodec = new HttpServerCodec();
        HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(serverCodec, protocol -> {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(multiplexCodec);
            } else {
                return null;
            }
        }, nettyHttpServerConfig.getMaxContentLength());
        return new CleartextHttp2ServerUpgradeHandler(serverCodec, upgradeHandler, multiplexCodec);
    }

    /**
     * A new upgrade handler.
     * Sadly, this does not work.
     */
    protected CleartextHttp2ServerUpgradeHandler createNewUpgradeHandler(NettyHttpServer nettyHttpServer,
                                                                         HttpAddress httpAddress,
                                                                         ServerNameIndicationHandler serverNameIndicationHandler) {
        NettyHttpServerConfig nettyHttpServerConfig = nettyHttpServer.getNettyHttpServerConfig();
        Https2ChildChannelInitializer childHandler =
                new Https2ChildChannelInitializer(nettyHttpServer, httpAddress, serverNameIndicationHandler);
        Http2FrameCodec frameCodec = Http2FrameCodecBuilder.forServer()
                .frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "server"))
                .initialSettings(Http2Settings.defaultSettings())
                .validateHeaders(true)
                .build();
        Http2MultiplexHandler multiplexHandler = new Http2MultiplexHandler(childHandler);
        HttpServerCodec serverCodec = new HttpServerCodec();
        HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(serverCodec, protocol -> {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(frameCodec, multiplexHandler);
            } else {
                return null;
            }
        }, nettyHttpServerConfig.getMaxContentLength());
        return new CleartextHttp2ServerUpgradeHandler(serverCodec, upgradeHandler, childHandler);
    }

}
