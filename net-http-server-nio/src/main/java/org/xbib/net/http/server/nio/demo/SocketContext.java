package org.xbib.net.http.server.nio.demo;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SocketContext {

    private final SocketChannel socketChannel;

    private final SelectionKey selectionKey;

    private final Thread thread;

    private final String connectionId;

    private final Selector selector;

    public SocketContext(SocketChannel socketChannel, SelectionKey selectionKey, Thread thread, String connectionId, Selector selector) {
        this.socketChannel = socketChannel;
        this.selectionKey = selectionKey;
        this.thread = thread;
        this.connectionId = connectionId;
        this.selector = selector;
    }

    public void close() throws IOException {
        this.socketChannel.close();
        this.thread.interrupt();
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public Thread getThread() {
        return thread;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Selector getSelector() {
        return selector;
    }

}
