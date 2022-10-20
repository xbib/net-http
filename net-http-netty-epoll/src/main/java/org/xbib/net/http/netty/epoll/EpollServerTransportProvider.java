package org.xbib.net.http.netty.epoll;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import org.xbib.net.http.server.netty.ServerTransportProvider;

import java.util.concurrent.ThreadFactory;

public class EpollServerTransportProvider implements ServerTransportProvider {

    public EpollServerTransportProvider() {
    }

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return Epoll.isAvailable() ? new EpollEventLoopGroup(nThreads, threadFactory) : null;
    }

    @Override
    public Class<? extends ServerSocketChannel> createServerSocketChannelClass() {
        return Epoll.isAvailable() ? EpollServerSocketChannel.class : null;
    }
}
