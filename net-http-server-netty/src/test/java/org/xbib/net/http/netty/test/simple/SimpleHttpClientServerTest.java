package org.xbib.net.http.netty.test.simple;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

class SimpleHttpClientServerTest {

    private static final Logger clientLogger = Logger.getLogger("client");

    private static final Logger serverLogger = Logger.getLogger("server");

    private static final LogLevel logLevel = LogLevel.DEBUG;

    private static final Level level = Level.FINE;

    @Test
    void testHttp() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 8008);
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        EventLoopGroup serverEventLoopGroup = new NioEventLoopGroup();
        EventLoopGroup clientEventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(serverEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast("server-traffic", new TrafficLoggingHandler("server-traffic", logLevel))
                                    .addLast("server-codec", new HttpServerCodec())
                                    .addLast("server-http-aggregator", new HttpObjectAggregator(256 * 1024))
                                    .addLast("server-handler", new ServerHandler());
                        }
                    });
            Channel serverChannel = serverBootstrap.bind(inetSocketAddress).sync().channel();
            serverLogger.log(level, "server up, channel = " + serverChannel);

            Bootstrap clientBootstrap = new Bootstrap()
                    .group(clientEventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast("client-traffic", new TrafficLoggingHandler("client-traffic", logLevel))
                                    .addLast("client-codec", new HttpClientCodec())
                                    .addLast("client-http-aggregator", new HttpObjectAggregator(256 * 1024))
                                    .addLast("client-handler", new ClientHandler(completableFuture));
                        }
                    });
            Channel clientChannel = clientBootstrap.connect(inetSocketAddress).sync().channel();
            clientLogger.log(level, "client connected, channel = " + clientChannel);
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                    "/", Unpooled.wrappedBuffer("Hello World".getBytes(StandardCharsets.UTF_8)));
            request.headers().add(HttpHeaderNames.HOST, inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort());
            clientChannel.writeAndFlush(request);
            clientLogger.log(level, "waiting");
            completableFuture.get(30, TimeUnit.SECONDS);
            if (completableFuture.isDone()) {
                clientLogger.log(Level.INFO, "success");
            } else {
                fail();
            }
        } finally {
            clientEventLoopGroup.shutdownGracefully();
            serverEventLoopGroup.shutdownGracefully();
            clientLogger.log(level, "client shutdown");
            serverLogger.log(level, "server shutdown");
        }
    }

    private static class ClientHandler extends ChannelDuplexHandler {

        private final CompletableFuture<Boolean> completableFuture;

        ClientHandler(CompletableFuture<Boolean> completableFuture) {
            this.completableFuture = completableFuture;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof FullHttpResponse) {
                clientLogger.log(level, "msg received on client " + msg + " class=" + msg.getClass() + " completing future");
                completableFuture.complete(true);
            } else {
                clientLogger.log(level, "unknown msg received on client " + msg + " class=" + msg.getClass() );
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            clientLogger.log(level, " channel read complete");
            ctx.flush();
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            clientLogger.log(level, "got event on client " + evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            clientLogger.log(Level.WARNING, cause.getMessage(), cause);
            ctx.close();
        }
    }

    private static class ServerHandler extends ChannelDuplexHandler {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            serverLogger.log(level, "msg received on server " + msg + " class=" + msg.getClass());
            if (msg instanceof FullHttpRequest) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK);
                serverLogger.log(Level.INFO, "writing OK response: " + response);
                ctx.write(response);
            } else {
                super.channelRead(ctx, msg);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            // important to both flush and close the context
            ctx.flush();
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            serverLogger.log(level, "got event on server " + evt);
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            serverLogger.log(Level.WARNING, cause.getMessage(), cause);
            ctx.close();
        }
    }

    private static class TrafficLoggingHandler extends LoggingHandler {

        TrafficLoggingHandler(String name, LogLevel level) {
            super(name, level);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            ctx.fireChannelRegistered();
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) {
            ctx.fireChannelUnregistered();
        }

        @Override
        public void flush(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof ByteBuf && !((ByteBuf) msg).isReadable()) {
                ctx.write(msg, promise);
            } else {
                super.write(ctx, msg, promise);
            }
        }
    }
}
