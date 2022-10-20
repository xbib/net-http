package org.xbib.net.http.client.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import java.util.concurrent.ThreadFactory;

public interface ClientTransportProvider {

    EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory);

    Class<? extends SocketChannel> createSocketChannelClass();

}
