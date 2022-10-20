package org.xbib.net.http.client.netty.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.logging.LogLevel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.netty.HttpChannelInitializer;
import org.xbib.net.http.client.netty.Interaction;
import org.xbib.net.http.client.netty.NettyCustomizer;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.TrafficLoggingHandler;

public class Http2ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Http2ChannelInitializer.class.getName());

    public Http2ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress httpAddress) {
        return HttpVersion.HTTP_2_0.equals(httpAddress.getVersion()) && !httpAddress.isSecure();
    }

    @Override
    public Interaction newInteraction(NettyHttpClient client, HttpAddress httpAddress) {
        return new Http2Interaction(client, httpAddress);
    }

    @Override
    public void init(Channel channel,
                     HttpAddress httpAddress,
                     NettyHttpClient nettyHttpClient,
                     NettyCustomizer nettyCustomizer,
                     Interaction interaction) {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
        ChannelPipeline pipeline = channel.pipeline();
        if (nettyHttpClientConfig.isDebug()) {
            pipeline.addLast(new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        configurePlain(channel, nettyHttpClient, interaction);
        if (nettyCustomizer != null) {
            nettyCustomizer.afterChannelInitialized(channel);
        }
        if (nettyHttpClientConfig.isDebug()) {
            logger.log(Level.FINE, "HTTP/2 plain channel initialized: address = " + httpAddress +
                    " pipeline = " + pipeline.names());
        }
    }

    private void configurePlain(Channel channel,
                                NettyHttpClient nettyHttpClient,
                                Interaction interaction) {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
        ChannelInitializer<Channel> initializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                throw new IllegalStateException();
            }
        };
        Http2MultiplexCodecBuilder multiplexCodecBuilder = Http2MultiplexCodecBuilder.forClient(initializer)
                .initialSettings(nettyHttpClientConfig.getHttp2Settings());
        if (nettyHttpClientConfig.isDebug()) {
            multiplexCodecBuilder.frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "client-frame"));
        }
        Http2MultiplexCodec multiplexCodec = multiplexCodecBuilder
                .autoAckPingFrame(true)
                .autoAckSettingsFrame(true)
                .decoupleCloseAndGoAway(false)
                .gracefulShutdownTimeoutMillis(30000L)
                .build();
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("client-multiplex", multiplexCodec);
        pipeline.addLast("client-messages", new Http2Messages(interaction));
    }
}
