package org.xbib.net.http.server.netty.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
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
            HttpAddress httpAddress = ctx.channel().attr(NettyHttpServerConfig.ATTRIBUTE_KEY_HTTP_ADDRESS).get();
            try {
                Integer streamId = fullHttpRequest.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
                HttpResponseBuilder httpResponseBuilder = HttpResponse.builder()
                        .setChannelHandlerContext(ctx)
                        .setVersion(HttpVersion.HTTP_2_0);
                httpResponseBuilder.shouldClose("close".equalsIgnoreCase(fullHttpRequest.headers().get(HttpHeaderNames.CONNECTION)));
                if (streamId != null) {
                    httpResponseBuilder.setStreamId(streamId + 1);
                }
                HttpRequestBuilder serverRequestBuilder = HttpRequest.builder()
                        .setFullHttpRequest(fullHttpRequest)
                        .setBaseURL(httpAddress,
                                fullHttpRequest.uri(),
                                fullHttpRequest.headers().get(HttpHeaderNames.HOST))
                        .setLocalAddress((InetSocketAddress) ctx.channel().localAddress())
                        .setRemoteAddress((InetSocketAddress) ctx.channel().remoteAddress())
                        .setStreamId(streamId);
                nettyHttpServer.dispatch(serverRequestBuilder, httpResponseBuilder);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "bad request:" + e.getMessage(), e);
                DefaultFullHttpResponse fullHttpResponse =
                        new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.valueOf(httpAddress.getVersion().text()),
                                HttpResponseStatus.BAD_REQUEST);
                ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
            } finally {
                fullHttpRequest.release();
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
}
