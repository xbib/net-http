package org.xbib.net.http.netty.epoll;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import java.util.concurrent.ThreadFactory;
import org.xbib.net.http.client.netty.ClientTransportProvider;

public class EpollClientTransportProvider implements ClientTransportProvider {

    public EpollClientTransportProvider() {
    }

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return Epoll.isAvailable() ? new EpollEventLoopGroup(nThreads, threadFactory) : null;
    }

    @Override
    public Class<? extends SocketChannel> createSocketChannelClass() {
        return Epoll.isAvailable() ? EpollSocketChannel.class : null;
    }
}
