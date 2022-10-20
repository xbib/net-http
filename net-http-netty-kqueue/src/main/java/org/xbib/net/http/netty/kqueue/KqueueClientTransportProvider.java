package org.xbib.net.http.netty.kqueue;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.SocketChannel;
import java.util.concurrent.ThreadFactory;
import org.xbib.net.http.client.netty.ClientTransportProvider;

public class KqueueClientTransportProvider implements ClientTransportProvider {

    public KqueueClientTransportProvider() {
    }

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return KQueue.isAvailable() ? new KQueueEventLoopGroup(nThreads, threadFactory) : null;
    }

    @Override
    public Class<? extends SocketChannel> createSocketChannelClass() {
        return KQueue.isAvailable() ? KQueueSocketChannel.class : null;
    }
}
