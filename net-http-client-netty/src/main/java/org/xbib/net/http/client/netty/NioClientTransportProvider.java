package org.xbib.net.http.client.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;

public class NioClientTransportProvider implements ClientTransportProvider {

    public NioClientTransportProvider() {
    }

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new NioEventLoopGroup(nThreads, threadFactory);
    }

    @Override
    public Class<? extends SocketChannel> createSocketChannelClass() {
        return NioSocketChannel.class;
    }
}
