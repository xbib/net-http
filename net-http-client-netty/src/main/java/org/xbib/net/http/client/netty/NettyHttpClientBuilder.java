package org.xbib.net.http.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.util.NamedThreadFactory;

public class NettyHttpClientBuilder {

    private static final Logger logger = Logger.getLogger(NettyHttpClientBuilder.class.getName());

    NettyHttpClientConfig nettyHttpClientConfig;

    ByteBufAllocator byteBufAllocator;

    EventLoopGroup eventLoopGroup;

    Class<? extends SocketChannel> socketChannelClass;

    HttpChannelInitializer httpChannelInitializer;

    NettyCustomizer nettyCustomizer;

    NettyHttpClientBuilder() {
    }

    public NettyHttpClientBuilder setConfig(NettyHttpClientConfig nettyHttpClientConfig) {
        this.nettyHttpClientConfig = nettyHttpClientConfig;
        return this;
    }

    /**
     * Set Netty's ByteBuf allocator.
     *
     * @param byteBufAllocator the byte buf allocator
     * @return this builder
     */
    public NettyHttpClientBuilder setByteBufAllocator(ByteBufAllocator byteBufAllocator) {
        this.byteBufAllocator = byteBufAllocator;
        return this;
    }

    public NettyHttpClientBuilder setEventLoop(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return this;
    }

    public NettyHttpClientBuilder setChannelClass(Class<SocketChannel> socketChannelClass) {
        this.socketChannelClass = socketChannelClass;
        return this;
    }

    public NettyHttpClientBuilder addPoolNode(HttpAddress httpAddress) {
        nettyHttpClientConfig.addPoolNode(httpAddress);
        nettyHttpClientConfig.setPoolVersion(httpAddress.getVersion());
        nettyHttpClientConfig.setPoolSecure(httpAddress.isSecure());
        return this;
    }

    public NettyHttpClientBuilder setHttpChannelInitializer(HttpChannelInitializer httpChannelInitializer) {
        this.httpChannelInitializer = httpChannelInitializer;
        return this;
    }

    public NettyHttpClientBuilder setNettyCustomizer(NettyCustomizer nettyCustomizer) {
        this.nettyCustomizer = nettyCustomizer;
        return this;
    }

    public NettyHttpClient build() throws IOException {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "installed security providers = " +
                    Arrays.stream(Security.getProviders()).map(Provider::getName).collect(Collectors.toList()));
        }
        if (nettyHttpClientConfig == null) {
            nettyHttpClientConfig = createEmptyConfig();
        }
        if (byteBufAllocator == null) {
            byteBufAllocator = ByteBufAllocator.DEFAULT;
        }
        EventLoopGroup myEventLoopGroup = createEventLoopGroup(nettyHttpClientConfig, eventLoopGroup);
        Class<? extends SocketChannel> mySocketChannelClass = createChannelClass(nettyHttpClientConfig, socketChannelClass);
        Bootstrap bootstrap = createBootstrap(nettyHttpClientConfig, byteBufAllocator, myEventLoopGroup, mySocketChannelClass);
        if (nettyCustomizer != null) {
            nettyCustomizer.afterBootstrapInitialized(bootstrap);
        }
        return new NettyHttpClient(this, myEventLoopGroup, bootstrap);
    }

    protected NettyHttpClientConfig createEmptyConfig() {
        return new NettyHttpClientConfig();
    }

    private static EventLoopGroup createEventLoopGroup(NettyHttpClientConfig clientConfig,
                                                       EventLoopGroup eventLoopGroup) {
        if (eventLoopGroup != null) {
            return eventLoopGroup;
        }
        EventLoopGroup myEventLoopGroup = null;
        ThreadFactory threadFactory = new NamedThreadFactory("org-xbib-net-http-netty-client");
        ServiceLoader<ClientTransportProvider> transportProviders = ServiceLoader.load(ClientTransportProvider.class);
        for (ClientTransportProvider serverTransportProvider : transportProviders) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "found event loop group provider = " + serverTransportProvider);
            }
            if (clientConfig.getTransportProviderName() == null || clientConfig.getTransportProviderName().equals(serverTransportProvider.getClass().getName())) {
                myEventLoopGroup = serverTransportProvider.createEventLoopGroup(clientConfig.getThreadCount(), threadFactory);
                break;
            }
        }
        if (myEventLoopGroup == null) {
            myEventLoopGroup = new NioEventLoopGroup(clientConfig.getThreadCount(), threadFactory);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "event loop group class: " + myEventLoopGroup.getClass().getName());
        }
        return myEventLoopGroup;
    }

    private static Class<? extends SocketChannel> createChannelClass(NettyHttpClientConfig clientConfig,
                                                                     Class<? extends SocketChannel> socketChannelClass) {
        if (socketChannelClass != null) {
            return socketChannelClass;
        }
        Class<? extends SocketChannel> myChannelClass = null;
        ServiceLoader<ClientTransportProvider> transportProviders = ServiceLoader.load(ClientTransportProvider.class);
        for (ClientTransportProvider transportProvider : transportProviders) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "found socket channel provider = " + transportProvider);
            }
            if (clientConfig.getTransportProviderName() == null || clientConfig.getTransportProviderName().equals(transportProvider.getClass().getName())) {
                myChannelClass = transportProvider.createSocketChannelClass();
                break;
            }
        }
        if (myChannelClass == null) {
            myChannelClass = NioSocketChannel.class;
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "socket channel class: " + myChannelClass.getName());
        }
        return myChannelClass;
    }

    private static Bootstrap createBootstrap(NettyHttpClientConfig nettyHttpClientConfig,
                                             ByteBufAllocator byteBufAllocator,
                                             EventLoopGroup eventLoopGroup,
                                             Class<? extends SocketChannel> socketChannelClass) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .channel(socketChannelClass)
                .option(ChannelOption.ALLOCATOR, byteBufAllocator)
                .option(ChannelOption.TCP_NODELAY, nettyHttpClientConfig.socketConfig.isTcpNodelay())
                .option(ChannelOption.SO_KEEPALIVE, nettyHttpClientConfig.socketConfig.isKeepAlive())
                .option(ChannelOption.SO_REUSEADDR, nettyHttpClientConfig.socketConfig.isReuseAddr())
                .option(ChannelOption.SO_LINGER, nettyHttpClientConfig.socketConfig.getLinger())
                .option(ChannelOption.SO_SNDBUF, nettyHttpClientConfig.socketConfig.getTcpSendBufferSize())
                .option(ChannelOption.SO_RCVBUF, nettyHttpClientConfig.socketConfig.getTcpReceiveBufferSize())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyHttpClientConfig.socketConfig.getConnectTimeoutMillis())
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, nettyHttpClientConfig.getWriteBufferWaterMark());
    }
}
