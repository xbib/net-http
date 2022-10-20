package org.xbib.net.http.server.nio.demo;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class EventLoop {

    private final Selector selector;
    private final CountDownLatch countDownLatch;
    private SocketHandlerProvider socketHandlerProvider;

    public EventLoop(CountDownLatch countDownLatch) throws IOException {
        this.selector = Selector.open();
        this.countDownLatch = countDownLatch;
    }

    public synchronized void add(SocketChannel socketChannel) throws IOException {
        this.add(socketChannel, this.socketHandlerProvider);
    }

    public void add(SocketChannel socketChannel, SocketHandlerProvider socketHandlerProvider) throws IOException {
        SelectionKey key = socketChannel
                .configureBlocking(false)
                .register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        String connectionId = UUID.randomUUID().toString();
        SocketContext socketContext = new SocketContext(socketChannel, key, Thread.currentThread(), connectionId, selector);
        SocketHandler handler = socketHandlerProvider.provide(socketContext);
        key.attach(handler);
        handler.onRegistered();
        selector.wakeup();
    }

    public CompletableFuture<String> loop() {
        countDownLatch.countDown();
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    SocketHandler handler = (SocketHandler) key.attachment();
                    try {
                        if (key.isReadable()) {
                            handler.onRead();
                        } else if (key.isWritable()) {
                            handler.onWrite();
                        }
                    } catch (IOException e) {
                        key.channel().close();
                    }
                    iter.remove();
                }
            } catch (Exception e) {
                return CompletableFuture.supplyAsync(() -> {
                    throw new RuntimeException(e);
                });
            }
        }
    }

    public void setSocketHandlerProvider(SocketHandlerProvider socketHandlerProvider) {
        this.socketHandlerProvider = socketHandlerProvider;
    }
}
