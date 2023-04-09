package org.xbib.net.http.server.netty.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.netty.HttpRequest;
import org.xbib.net.http.server.netty.HttpRequestBuilder;
import org.xbib.net.http.server.netty.HttpResponse;
import org.xbib.net.http.server.netty.HttpResponseBuilder;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;

@ChannelHandler.Sharable
public class Http2Handler extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Http2Handler.class.getName());

    private final NettyHttpServer nettyHttpServer;

    public Http2Handler(NettyHttpServer nettyHttpServer) {
        this.nettyHttpServer = nettyHttpServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws IOException {
        if (object instanceof FullHttpRequest fullHttpRequest) {
            HttpAddress httpAddress = ctx.channel().attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_ADDRESS).get();
            try {
                Integer streamId = fullHttpRequest.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
                HttpResponseBuilder httpResponseBuilder = HttpResponse.builder()
                        .setChannelHandlerContext(ctx)
                        .setVersion(HttpVersion.HTTP_2_0);
                httpResponseBuilder.shouldClose("close".equalsIgnoreCase(fullHttpRequest.headers().get(HttpHeaderNames.CONNECTION)));
                if (streamId != null) {
                    httpResponseBuilder.setStreamId(streamId + 1);
                }
                ctx.channel().attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_RESPONSE).set(httpResponseBuilder);
                final InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
                final InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                HttpRequestBuilder httpRequestBuilder = HttpRequest.builder()
                        .setFullHttpRequest(fullHttpRequest)
                        .setBaseURL(httpAddress,
                                fullHttpRequest.uri(),
                                fullHttpRequest.headers().get(HttpHeaderNames.HOST))
                        .setLocalAddress(localAddress)
                        .setRemoteAddress(remoteAddress)
                        .setStreamId(streamId);
                ctx.channel().attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_REQUEST).set(httpRequestBuilder);
                logger.log(Level.FINEST, () -> "incoming connection: " + remoteAddress + " -> " + localAddress);
                nettyHttpServer.dispatch(httpRequestBuilder, httpResponseBuilder);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "bad request:" + e.getMessage(), e);
                DefaultFullHttpResponse fullHttpResponse =
                        new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.valueOf(httpAddress.getVersion().text()),
                                HttpResponseStatus.BAD_REQUEST);
                ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.SEVERE, cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();
        HttpRequestBuilder httpRequest = ch.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_REQUEST).get();
        if (httpRequest != null) {
            logger.log(Level.FINEST, "releasing HttpRequestBuilder");
            httpRequest.release();
        }
        HttpResponseBuilder httpResponse = ch.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_RESPONSE).get();
        if (httpResponse != null) {
            logger.log(Level.FINEST, "releasing HttpResponseBuilder");
            httpResponse.release();
        }
        HttpDataFactory httpDataFactory = ch.attr(NettyHttpServerConfig.ATTRIBUTE_HTTP_DATAFACTORY).get();
        if (httpDataFactory != null) {
            logger.log(Level.FINEST, "cleaning http data factory");
            httpDataFactory.cleanAllHttpData();
        }
    }
}
