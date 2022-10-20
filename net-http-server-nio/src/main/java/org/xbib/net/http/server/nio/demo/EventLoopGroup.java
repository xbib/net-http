package org.xbib.net.http.server.nio.demo;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EventLoopGroup {

    private final List<EventLoop> loops;

    private int position = 0;

    public EventLoopGroup(int size) throws IOException, InterruptedException {
        this.loops = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            EventLoop loop = new EventLoop(countDownLatch);
            Thread thread = new Thread(loop::loop);
            thread.setName("event-loop-" + i);
            thread.start();
            this.loops.add(loop);
        }
        boolean await = countDownLatch.await(10, TimeUnit.SECONDS);
        if (!await) {
            throw new RuntimeException("count down latch await timeout");
        }
    }

    public void dispatch(SocketChannel socketChannel, SocketHandlerProvider socketHandlerProvider) throws IOException {
        if (position >= loops.size()) {
            position = 0;
        }
        EventLoop eventLoop = loops.get(position);
        eventLoop.add(socketChannel, socketHandlerProvider);
        position++;
    }

    public void addEventLoop() throws IOException, InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        EventLoop loop = new EventLoop(countDownLatch);
        Thread thread = new Thread(loop::loop);
        thread.setName("event-loop-" + loops.size());
        thread.start();
        boolean await = countDownLatch.await(10, TimeUnit.SECONDS);
        if (!await) {
            throw new RuntimeException("count down latch await timeout");
        }
    }
}
