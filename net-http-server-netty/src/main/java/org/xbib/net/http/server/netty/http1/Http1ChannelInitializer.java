package org.xbib.net.http.server.netty.http1;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.stream.ChunkedWriteHandler;
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
 * Insecure HTTP 1.1 server channel initializer. This channel initializer can not upgrade to HTTP 2.0.
 */
public class Http1ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Http1ChannelInitializer.class.getName());

    public Http1ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress address) {
        return org.xbib.net.http.HttpVersion.HTTP_1_1.equals(address.getVersion()) && !address.isSecure();
    }

    @Override
    public void init(Channel channel, NettyHttpServer server, NettyCustomizer customizer) {
        NettyHttpServerConfig nettyHttpServerConfig = server.getNettyHttpServerConfig();
        ChannelPipeline pipeline = channel.pipeline();
        if (nettyHttpServerConfig.isDebug()) {
            pipeline.addLast("server-logging", new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        // always use the server codec or we won't be able to handle HTTP
        pipeline.addLast("server-codec", new HttpServerCodec(nettyHttpServerConfig.getMaxInitialLineLength(),
                        nettyHttpServerConfig.getMaxHeadersSize(), nettyHttpServerConfig.getMaxChunkSize()));
        if (nettyHttpServerConfig.isCompressionEnabled()) {
            pipeline.addLast("server-compressor", new HttpContentCompressor());
        }
        if (nettyHttpServerConfig.isDecompressionEnabled()) {
            pipeline.addLast("server-decompressor", new HttpContentDecompressor());
        }
        if (nettyHttpServerConfig.isObjectAggregationEnabled()) {
            HttpObjectAggregator httpObjectAggregator = new HttpObjectAggregator(nettyHttpServerConfig.getMaxContentLength());
            httpObjectAggregator.setMaxCumulationBufferComponents(nettyHttpServerConfig.getMaxCompositeBufferComponents());
            pipeline.addLast("server-aggregator", httpObjectAggregator);
        }
        if (nettyHttpServerConfig.isChunkedWriteEnabled()) {
            pipeline.addLast("server-chunked-write", new ChunkedWriteHandler());
        }
        if (nettyHttpServerConfig.isFileUploadEnabled()) {
            HttpFileUploadHandler httpFileUploadHandler = new HttpFileUploadHandler(server);
            pipeline.addLast("server-file-upload", httpFileUploadHandler);
        }
        if (nettyHttpServerConfig.isPipeliningEnabled()) {
            pipeline.addLast("server-pipelining", new HttpPipeliningHandler(nettyHttpServerConfig.getPipeliningCapacity()));
        }
        pipeline.addLast("server-handler", new Http1Handler(server));
        pipeline.addLast("server-idle-timeout", new IdleTimeoutHandler(nettyHttpServerConfig.getTimeoutMillis()));
        if (customizer != null) {
            customizer.afterChannelInitialized(channel);
        }
        if (nettyHttpServerConfig.isDebug() && logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "HTTP/1.1 server channel initialized: address=" + channel.localAddress() +
                    " pipeline=" + channel.pipeline().names());
        }
    }
}
