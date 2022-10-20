package org.xbib.net.http.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Strategy interface to customize netty {@link Bootstrap} and {@link Channel} via callback hooks.
 * <b>Extending the NettyCustomizer API</b>
 * Contrary to other driver options, the options available in this class should be considered as advanced feature and as such,
 * they should only be modified by expert users. A misconfiguration introduced by the means of this API can have unexpected
 * results and cause the driver to completely fail to connect.
 */
public interface NettyCustomizer {

    /**
     * Hook invoked each time the driver creates a new Connection and configures a new instance of Bootstrap for it. This hook
     * is called after the driver has applied all {@link java.net.SocketOption}s. This is a good place to add extra
     * {@link io.netty.channel.ChannelOption}s to the {@link Bootstrap}.
     *
     * @param bootstrap must not be {@code null}.
     */
    default void afterBootstrapInitialized(Bootstrap bootstrap) {
    }

    /**
     * Hook invoked each time the driver initializes the channel. This hook is called after the driver has registered all its
     * internal channel handlers, and applied the configured options.
     *
     * @param channel must not be {@code null}.
     */
    default void afterChannelInitialized(Channel channel) {
    }

    /**
     * Hook invoked each time a full HTTP request is received in a Netty handler pipeline.
     * Useful to adjust headers in a Netty way.
     *
     * @param ctx the channel context
     * @param fullHttpRequest the full HTTP request
     */
    default void afterFullHttpRequestReceived(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
    }

}
