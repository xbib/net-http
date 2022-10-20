package org.xbib.net.http.server.netty.http1;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.LastHttpContent;

import java.nio.channels.ClosedChannelException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements HTTP pipelining ordering, ensuring that responses are completely served in the same order as their
 * corresponding requests.
 *
 * Based on <a href="https://github.com/typesafehub/netty-http-pipelining">https://github.com/typesafehub/netty-http-pipelining</a>
 * which uses Netty3.
 *
 * WARNING: this only works if there are no pipeline interuptions, for example by exceptions that force connection close.
 * In that case, the responses will be generated but can not be written. It looks like no message reaches the network.
 * This could be a bug.
 */
public class HttpPipeliningHandler extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(HttpPipeliningHandler.class.getName());

    private final int pipelineCapacity;

    private final Lock lock;

    private final Queue<HttpPipelinedResponse> httpPipelinedResponses;

    private static final AtomicInteger sequenceIdCounter = new AtomicInteger(0);

    private static final AtomicInteger writtenRequests = new AtomicInteger(0);

    /**
     * @param pipelineCapacity the maximum number of channel events that will be retained prior to aborting the channel
     *                      connection. This is required as events cannot queue up indefinitely; we would run out of
     *                      memory if this was the case.
     */
    public HttpPipeliningHandler(int pipelineCapacity) {

        this.pipelineCapacity = pipelineCapacity;
        this.lock = new ReentrantLock();
        this.httpPipelinedResponses = new PriorityQueue<>(1);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof LastHttpContent) {
            ctx.fireChannelRead(new HttpPipelinedRequest((LastHttpContent) msg, sequenceIdCounter.getAndIncrement()));
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpPipelinedResponse) {
            boolean channelShouldClose = false;
            lock.lock();
            try {
                if (httpPipelinedResponses.size() < pipelineCapacity) {
                    HttpPipelinedResponse httpPipelinedResponse = (HttpPipelinedResponse) msg;
                    httpPipelinedResponses.add(httpPipelinedResponse);
                    while (!httpPipelinedResponses.isEmpty()) {
                        HttpPipelinedResponse queuedPipelinedResponse = httpPipelinedResponses.peek();
                        if (queuedPipelinedResponse.getSequenceId() != writtenRequests.get()) {
                            break;
                        }
                        httpPipelinedResponses.remove();
                        super.write(ctx, queuedPipelinedResponse.getResponse(), queuedPipelinedResponse.getPromise());
                        writtenRequests.getAndIncrement();
                    }
                } else {
                    logger.log(Level.WARNING, "pipeline capacity exceeded, closing channel");
                    channelShouldClose = true;
                }
            } finally {
                lock.unlock();
            }
            if (channelShouldClose) {
                ctx.close();
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        if (!httpPipelinedResponses.isEmpty()) {
            ClosedChannelException closedChannelException = new ClosedChannelException();
            HttpPipelinedResponse pipelinedResponse;
            while ((pipelinedResponse = httpPipelinedResponses.poll()) != null) {
                try {
                    pipelinedResponse.release();
                    pipelinedResponse.getPromise().setFailure(closedChannelException);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "unexpected error while releasing pipelined http responses", e);
                }
            }
        }
        ctx.close(promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String message = cause.getMessage() == null ? "null" : cause.getMessage();
        logger.log(Level.WARNING, message, cause);
        ctx.close();
    }
}
