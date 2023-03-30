package org.xbib.net.http.server.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.HttpServerBuilder;
import org.xbib.net.util.NamedThreadFactory;

import java.util.ServiceLoader;
import java.util.concurrent.ThreadFactory;

public class NettyHttpServerBuilder implements HttpServerBuilder {

    NettyHttpServerConfig nettyHttpServerConfig;

    Application application;

    ByteBufAllocator byteBufAllocator;

    EventLoopGroup parentEventLoopGroup;

    EventLoopGroup childEventLoopGroup;

    Class<? extends ServerSocketChannel> socketChannelClass;

    HttpChannelInitializer httpChannelInitializer;

    NettyCustomizer nettyCustomizer;

    String secureSocketProviderName;

    protected NettyHttpServerBuilder() {
        this.byteBufAllocator = ByteBufAllocator.DEFAULT;
        this.nettyHttpServerConfig = new NettyHttpServerConfig();
    }

    public NettyHttpServerBuilder setHttpServerConfig(NettyHttpServerConfig nettyHttpServerConfig) {
        this.nettyHttpServerConfig = nettyHttpServerConfig;
        return this;
    }

    public NettyHttpServerBuilder setByteBufAllocator(ByteBufAllocator byteBufAllocator) {
        this.byteBufAllocator = byteBufAllocator;
        return this;
    }

    public NettyHttpServerBuilder setParentEventLoopGroup(EventLoopGroup parentEventLoopGroup) {
        this.parentEventLoopGroup = parentEventLoopGroup;
        return this;
    }

    public NettyHttpServerBuilder setChildEventLoopGroup(EventLoopGroup childEventLoopGroup) {
        this.childEventLoopGroup = childEventLoopGroup;
        return this;
    }

    public NettyHttpServerBuilder setChannelClass(Class<? extends ServerSocketChannel> socketChannelClass) {
        this.socketChannelClass = socketChannelClass;
        return this;
    }

    public NettyHttpServerBuilder setNettyCustomizer(NettyCustomizer nettyCustomizer) {
        this.nettyCustomizer = nettyCustomizer;
        return this;
    }

    public NettyHttpServerBuilder setHttpChannelInitializer(HttpChannelInitializer httpChannelInitializer) {
        this.httpChannelInitializer = httpChannelInitializer;
        return this;
    }

    public NettyHttpServerBuilder setSecureSocketProviderName(String name) {
        this.secureSocketProviderName = name;
        return this;
    }

    public NettyHttpServerBuilder setApplication(Application application) {
        this.application = application;
        return this;
    }

    public NettyHttpServer build() {
        return new NettyHttpServer(this,
                createParentEventLoopGroup(nettyHttpServerConfig, parentEventLoopGroup),
                createChildEventLoopGroup(nettyHttpServerConfig, childEventLoopGroup),
                createSocketChannelClass(nettyHttpServerConfig, socketChannelClass));
    }

    private static EventLoopGroup createParentEventLoopGroup(NettyHttpServerConfig httpServerConfig,
                                                                                     EventLoopGroup parentEventLoopGroup) {
        if (parentEventLoopGroup != null) {
            return parentEventLoopGroup;
        }
        EventLoopGroup eventLoopGroup = null;
        ThreadFactory threadFactory = new NamedThreadFactory("org-xbib-net-http-netty-server-parent");
        ServiceLoader<ServerTransportProvider> transportProviders = ServiceLoader.load(ServerTransportProvider.class);
        for (ServerTransportProvider serverTransportProvider : transportProviders) {
            if (httpServerConfig.getTransportProviderName() == null ||
                    httpServerConfig.getTransportProviderName().equals(serverTransportProvider.getClass().getName())) {
                eventLoopGroup = serverTransportProvider.createEventLoopGroup(httpServerConfig.getParentThreadCount(),
                        threadFactory);
            }
        }
        if (eventLoopGroup == null) {
            eventLoopGroup = new NioEventLoopGroup(httpServerConfig.getParentThreadCount(), threadFactory);
        }
        return eventLoopGroup;
    }

    private static EventLoopGroup createChildEventLoopGroup(NettyHttpServerConfig httpServerConfig,
                                                                                    EventLoopGroup childEventLoopGroup) {
        if (childEventLoopGroup != null) {
            return childEventLoopGroup;
        }
        EventLoopGroup eventLoopGroup = null;
        ThreadFactory threadFactory = new NamedThreadFactory("org-xbib-net-http-netty-server-child");
        ServiceLoader<ServerTransportProvider> transportProviders = ServiceLoader.load(ServerTransportProvider.class);
        for (ServerTransportProvider serverTransportProvider : transportProviders) {
            if (httpServerConfig.getTransportProviderName() == null ||
                    httpServerConfig.getTransportProviderName().equals(serverTransportProvider.getClass().getName())) {
                eventLoopGroup = serverTransportProvider.createEventLoopGroup(httpServerConfig.getChildThreadCount(),
                        threadFactory);
            }
        }
        if (eventLoopGroup == null) {
            eventLoopGroup = new NioEventLoopGroup(httpServerConfig.getChildThreadCount(), threadFactory);
        }
        return eventLoopGroup;
    }

    private static Class<? extends ServerSocketChannel> createSocketChannelClass(NettyHttpServerConfig httpServerConfig,
                                                                                 Class<? extends ServerSocketChannel> socketChannelClass) {
        if (socketChannelClass != null) {
            return socketChannelClass;
        }
        Class<? extends ServerSocketChannel> channelClass = null;
        ServiceLoader<ServerTransportProvider> transportProviders = ServiceLoader.load(ServerTransportProvider.class);
        for (ServerTransportProvider serverTransportProvider : transportProviders) {
            if (httpServerConfig.getTransportProviderName() == null || httpServerConfig.getTransportProviderName().equals(serverTransportProvider.getClass().getName())) {
                channelClass = serverTransportProvider.createServerSocketChannelClass();
                break;
            }
        }
        if (channelClass == null) {
            channelClass = NioServerSocketChannel.class;
        }
        return channelClass;
    }
}
