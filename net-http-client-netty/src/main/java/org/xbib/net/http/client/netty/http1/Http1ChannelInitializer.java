package org.xbib.net.http.client.netty.http1;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
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

public class Http1ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Http1ChannelInitializer.class.getName());

    public Http1ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress httpAddress) {
        return HttpVersion.HTTP_1_1.equals(httpAddress.getVersion()) && !httpAddress.isSecure();
    }

    @Override
    public Interaction newInteraction(NettyHttpClient client, HttpAddress httpAddress) {
        return new Http1Interaction(client, httpAddress);
    }

    @Override
    public void init(Channel channel,
                     HttpAddress httpAddress,
                     NettyHttpClient nettyHttpClient,
                     NettyCustomizer nettyCustomizer,
                     Interaction interaction) throws IOException {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
        ChannelPipeline pipeline = channel.pipeline();
        if (nettyHttpClientConfig.isDebug()) {
            pipeline.addLast("client-traffic", new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        int readTimeoutMilllis = nettyHttpClientConfig.getSocketConfig().getReadTimeoutMillis();
        if (readTimeoutMilllis > 0) {
            pipeline.addLast("client-read-timeout", new ReadTimeoutHandler(readTimeoutMilllis / 1000));
        }
        int socketTimeoutMillis = nettyHttpClientConfig.getSocketConfig().getSocketTimeoutMillis();
        if (socketTimeoutMillis > 0) {
            pipeline.addLast("client-idle-timeout", new IdleStateHandler(socketTimeoutMillis / 1000,
                    socketTimeoutMillis / 1000, socketTimeoutMillis / 1000));
        }
        if (nettyHttpClientConfig.getHttpProxyHandler() != null) {
            pipeline.addLast("client-http-proxy", nettyHttpClientConfig.getHttpProxyHandler());
        }
        if (nettyHttpClientConfig.getSocks4ProxyHandler() != null) {
            pipeline.addLast("client-socks4-proxy", nettyHttpClientConfig.getSocks4ProxyHandler());
        }
        if (nettyHttpClientConfig.getSocks5ProxyHandler() != null) {
            Socks5ProxyHandler socks5ProxyHandler = nettyHttpClientConfig.getSocks5ProxyHandler();
            pipeline.addLast("client-socks5-proxy", socks5ProxyHandler);
        }
        configurePlain(channel, nettyHttpClient, interaction);
        if (nettyCustomizer != null) {
            nettyCustomizer.afterChannelInitialized(channel);
        }
        if (nettyHttpClientConfig.isDebug()) {
            logger.log(Level.FINE, "HTTP 1.1 plain channel initialized: " +
                    " address=" + httpAddress +
                    " pipeline=" + pipeline.names());
        }
    }

    private void configurePlain(Channel channel,
                                NettyHttpClient nettyHttpClient,
                                Interaction interaction) throws IOException {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("http-client-codec", new HttpClientCodec(nettyHttpClientConfig.getMaxInitialLineLength(),
                nettyHttpClientConfig.getMaxHeadersSize(), nettyHttpClientConfig.getMaxChunkSize()));
        if (nettyHttpClientConfig.isGzipEnabled()) {
            pipeline.addLast("http-client-decompressor", new HttpContentDecompressor());
        }
        if (nettyHttpClientConfig.isObjectAggregationEnabled()) {
            HttpObjectAggregator httpObjectAggregator =
                    new HttpObjectAggregator(nettyHttpClientConfig.getMaxContentLength(), false);
            httpObjectAggregator.setMaxCumulationBufferComponents(nettyHttpClientConfig.getMaxCompositeBufferComponents());
            pipeline.addLast("http-client-aggregator", httpObjectAggregator);
        }
        if (nettyHttpClientConfig.isChunkWriteEnabled()) {
            //pipeline.addLast("http-client-chunk-content-compressor", new HttpChunkContentCompressor());
            pipeline.addLast("http-client-chunked-writer", new ChunkedWriteHandler());
        }
        pipeline.addLast("http-client-response", new Http1Handler(interaction));
        interaction.settingsReceived(null);
    }
}
