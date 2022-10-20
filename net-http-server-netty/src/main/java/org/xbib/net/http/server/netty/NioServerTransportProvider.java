package org.xbib.net.http.server.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ThreadFactory;

public class NioServerTransportProvider implements ServerTransportProvider {

    public NioServerTransportProvider() {
    }

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new NioEventLoopGroup(nThreads, threadFactory);
    }

    @Override
    public Class<? extends ServerSocketChannel> createServerSocketChannelClass() {
        return NioServerSocketChannel.class;
    }
}
