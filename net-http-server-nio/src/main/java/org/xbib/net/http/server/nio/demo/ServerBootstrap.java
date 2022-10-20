package org.xbib.net.http.server.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ServerBootstrap {

    private SocketHandlerProvider socketHandlerProvider;

    ServerBootstrap() {
    }

    public ServerBootstrap provider(SocketHandlerProvider socketHandlerProvider) {
        this.socketHandlerProvider = socketHandlerProvider;
        return this;
    }

    public void connect(int port) throws IOException, InterruptedException {
        if (socketHandlerProvider == null) {
            throw new IllegalArgumentException("socketHandlerProvider is null");
        }
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        EventLoopGroup eventLoopGroup = new EventLoopGroup(4);
        serverSocketChannel.bind(new InetSocketAddress(port))
                .configureBlocking(false)
                .register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    eventLoopGroup.dispatch(socketChannel, socketHandlerProvider);
                }
                iterator.remove();
            }
        }
    }
}
