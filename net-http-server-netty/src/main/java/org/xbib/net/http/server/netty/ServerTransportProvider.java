package org.xbib.net.http.server.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import java.util.concurrent.ThreadFactory;

public interface ServerTransportProvider {

    EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory);

    Class<? extends ServerSocketChannel> createServerSocketChannelClass();

}
