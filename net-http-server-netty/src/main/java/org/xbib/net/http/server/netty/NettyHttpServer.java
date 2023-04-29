package org.xbib.net.http.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.NetworkClass;
import org.xbib.net.NetworkUtils;
import org.xbib.net.SocketConfig;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.HttpServer;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.route.HttpRouter;

/**
 * Netty HTTP server.
 */
public class NettyHttpServer implements HttpServer {

    private static final Logger logger = Logger.getLogger(NettyHttpServer.class.getName());

    private final NettyHttpServerBuilder builder;

    private final EventLoopGroup parentEventLoopGroup;

    private final EventLoopGroup childEventLoopGroup;

    private final Class<? extends ServerSocketChannel> socketChannelClass;

    private final HttpChannelInitializer httpChannelInitializer;

    private final ServiceLoader<HttpChannelInitializer> serviceLoader;

    private final Collection<ChannelFuture> channelFutures;

    private final Collection<Channel> channels;

    NettyHttpServer(NettyHttpServerBuilder builder,
                    EventLoopGroup parentEventLoopGroup,
                    EventLoopGroup childEventLoopGroup,
                    Class<? extends ServerSocketChannel> socketChannelClass) {
        this.builder = builder;
        this.parentEventLoopGroup = parentEventLoopGroup;
        this.childEventLoopGroup = childEventLoopGroup;
        this.socketChannelClass = socketChannelClass;
        this.httpChannelInitializer = builder.httpChannelInitializer;
        this.serviceLoader = ServiceLoader.load(HttpChannelInitializer.class);
        this.channelFutures = new ArrayList<>();
        this.channels = new ArrayList<>();
        logger.log(Level.FINE, "parent event loop group = " + parentEventLoopGroup +
                " child event loop group = " + childEventLoopGroup  +
                " socket channel class = " + socketChannelClass +
                " router addresses = " + builder.application.getAddresses());
    }

    public static NettyHttpServerBuilder builder() {
        return new NettyHttpServerBuilder();
    }

    public NettyHttpServer getServer() {
        return this;
    }

    public NettyHttpServerConfig getNettyHttpServerConfig() {
        return builder.nettyHttpServerConfig;
    }

    @Override
    public void bind() throws BindException {
        Set<HttpAddress> httpAddressSet = builder.application.getAddresses();
        logger.log(Level.FINE, "http addresses = " + httpAddressSet);
        for (HttpAddress httpAddress : httpAddressSet) {
            SocketConfig socketConfig = httpAddress.getSocketConfig();
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(parentEventLoopGroup, childEventLoopGroup)
                .channel(socketChannelClass)
                .option(ChannelOption.ALLOCATOR, builder.byteBufAllocator)
                .option(ChannelOption.SO_REUSEADDR, socketConfig.isReuseAddr())
                .option(ChannelOption.SO_RCVBUF, socketConfig.getTcpReceiveBufferSize())
                .option(ChannelOption.SO_BACKLOG, socketConfig.getBackLogSize())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, socketConfig.getConnectTimeoutMillis())
                .childOption(ChannelOption.ALLOCATOR, builder.byteBufAllocator)
                .childOption(ChannelOption.SO_REUSEADDR, socketConfig.isReuseAddr())
                .childOption(ChannelOption.TCP_NODELAY, socketConfig.isTcpNodelay())
                .childOption(ChannelOption.SO_SNDBUF, socketConfig.getTcpSendBufferSize())
                .childOption(ChannelOption.SO_RCVBUF, socketConfig.getTcpReceiveBufferSize())
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, socketConfig.getConnectTimeoutMillis())
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.closeFuture().addListener((ChannelFuture future) -> {
                            Channel ch = future.channel();
                            HttpRequestBuilder httpRequest = ch.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_REQUEST).get();
                            if (httpRequest != null) {
                                logger.log(Level.FINEST, "releasing HttpRequestBuilder");
                                httpRequest.release();
                            }
                            HttpResponseBuilder httpResponse = ch.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_RESPONSE).get();
                            if (httpResponse != null) {
                                logger.log(Level.FINEST, "releasing HttpResponseBuilder");
                                httpResponse.release();
                            }
                            HttpDataFactory httpDataFactory = ch.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_DATAFACTORY).get();
                            if (httpDataFactory != null) {
                                logger.log(Level.FINEST, "cleaning http data factory");
                                httpDataFactory.cleanAllHttpData();
                            }
                        });
                        channel.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_ADDRESS).set(httpAddress);
                        createChannelInitializer(httpAddress).init(channel, getServer(), builder.nettyCustomizer);
                    }
                });
            if (getNettyHttpServerConfig().isDebug()) {
                bootstrap.handler(new LoggingHandler("server-logging", LogLevel.DEBUG));
            }
            if (builder.nettyCustomizer != null) {
                builder.nettyCustomizer.afterServerBootstrapInitialized(bootstrap);
            }
            try {
                InetSocketAddress inetSocketAddress = httpAddress.getInetSocketAddress();
                NetworkClass configuredNetworkClass = getNettyHttpServerConfig().getNetworkClass();
                NetworkClass detectedNetworkClass = NetworkUtils.getNetworkClass(inetSocketAddress.getAddress());
                if (!NetworkUtils.matchesNetwork(detectedNetworkClass, configuredNetworkClass)) {
                    throw new IOException("unable to bind to " + httpAddress + " because network class " +
                            detectedNetworkClass + " is not allowed by configured network class " + configuredNetworkClass);
                }
                logger.log(Level.FINE, () -> "trying to bind to " + inetSocketAddress);
                channelFutures.add(bootstrap.bind(inetSocketAddress));
            } catch (IOException e) {
                throw new BindException(e.getMessage());
            }
        }
        for (ChannelFuture channelFuture : channelFutures) {
            try {
                channelFuture.channel().closeFuture()
                        .addListener((ChannelFutureListener) future -> {
                            future.await();
                            logger.log(Level.FINER, "future " + future + " awaited");
                        });
                channels.add(channelFuture.sync().channel());
                channelFuture.await();
                logger.log(Level.FINE, () -> channelFuture.channel() + " ready, listening");
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    @Override
    public void loop() {
        for (Channel channel : channels) {
            try {
                ChannelFuture channelFuture = channel.closeFuture().sync();
                if (channelFuture.isDone()) {
                    logger.log(Level.FINER, () -> channel + " close future synced");
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    @Override
    public void dispatch(HttpRequestBuilder requestBuilder,
                         HttpResponseBuilder responseBuilder) {
        Callable<?> callable = (Callable<Object>) () -> {
            HttpRouter router = builder.application.getRouter();
            router.route(builder.application, requestBuilder, responseBuilder);
            return true;
        };
        builder.application.getExecutor().execute(callable);
    }

    @Override
    public void dispatch(HttpRequestBuilder requestBuilder,
                         HttpResponseBuilder responseBuilder,
                         HttpResponseStatus responseStatus) {
        Callable<?> callable = (Callable<Object>) () -> {
            HttpRouter router = builder.application.getRouter();
            HttpRouterContext httpRouterContext = builder.application.createContext(null, requestBuilder, responseBuilder);
            router.routeStatus(responseStatus, httpRouterContext);
            return true;
        };
        builder.application.getExecutor().execute(callable);
    }

    @Override
    public Collection<HttpDomain> getDomains() {
        return builder.application.getDomains();
    }

    @Override
    public void close() throws IOException {
        shutdownGracefully(30L, TimeUnit.SECONDS);
    }

    public void shutdownGracefully(long amount, TimeUnit timeUnit) throws IOException {
        logger.log(Level.INFO, "server shutting down");
        // shut down child event loop group, then parent event  loop group, then channel futures
        childEventLoopGroup.shutdownGracefully(1L, amount, timeUnit);
        try {
            if (!childEventLoopGroup.awaitTermination(amount, timeUnit)) {
                logger.log(Level.WARNING, "timeout");
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "timeout");
        }
        parentEventLoopGroup.shutdownGracefully(1L, amount, timeUnit);
        try {
            if (!parentEventLoopGroup.awaitTermination(amount, timeUnit)) {
                logger.log(Level.WARNING, "timeout");
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "timeout");
        }
        for (ChannelFuture channelFuture : channelFutures) {
            if (channelFuture != null && !channelFuture.isDone()) {
                if (channelFuture.channel().isOpen()) {
                    logger.log(Level.FINER, "closing channel future");
                    channelFuture.channel().close();
                }
                channelFuture.cancel(true);
            }
        }
        builder.application.close();
        logger.log(Level.INFO, "server shutdown complete");
    }

    private HttpChannelInitializer createChannelInitializer(HttpAddress address) {
        if (httpChannelInitializer != null && httpChannelInitializer.supports(address)) {
            return httpChannelInitializer;
        }
        for (HttpChannelInitializer httpChannelInitializer : serviceLoader) {
            if (httpChannelInitializer.supports(address)) {
                return httpChannelInitializer;
            }
        }
        throw new IllegalStateException("no channel initializer found for address " + address);
    }
}
