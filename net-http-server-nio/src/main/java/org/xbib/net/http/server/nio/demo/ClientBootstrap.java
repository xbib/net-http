package org.xbib.net.http.server.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class ClientBootstrap {

    private InetSocketAddress inetSocketAddress;
    private EventLoop eventLoop;
    private SocketHandlerProvider socketHandlerProvider;

    ClientBootstrap() {
    }

    public CompletableFuture<String> connect(InetSocketAddress inetSocketAddress) throws IOException {
        if (socketHandlerProvider == null) {
            throw new RuntimeException("socketHandlerProvider is null");
        }
        this.inetSocketAddress = inetSocketAddress;
        SocketChannel socketChannel = SocketChannel.open(inetSocketAddress);
        this.eventLoop = new EventLoop(new CountDownLatch(0));
        this.eventLoop.setSocketHandlerProvider(socketHandlerProvider);
        this.eventLoop.add(socketChannel);
        return this.eventLoop.loop();
    }

    public void setSocketHandlerProvider(SocketHandlerProvider socketHandlerProvider) {
        this.socketHandlerProvider = socketHandlerProvider;
    }
}
