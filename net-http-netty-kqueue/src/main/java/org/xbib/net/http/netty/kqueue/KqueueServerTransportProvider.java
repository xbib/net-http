package org.xbib.net.http.netty.kqueue;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import org.xbib.net.http.server.netty.ServerTransportProvider;
import java.util.concurrent.ThreadFactory;

public class KqueueServerTransportProvider implements ServerTransportProvider {

    public KqueueServerTransportProvider() {
    }

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return KQueue.isAvailable() ? new KQueueEventLoopGroup(nThreads, threadFactory) : null;
    }

    @Override
    public Class<? extends ServerSocketChannel> createServerSocketChannelClass() {
        return KQueue.isAvailable() ? KQueueServerSocketChannel.class : null;
    }
}
