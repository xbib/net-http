package org.xbib.net.http.server.netty.http1;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.HttpRequest;
import org.xbib.net.http.server.netty.HttpRequestBuilder;
import org.xbib.net.http.server.netty.HttpResponse;
import org.xbib.net.http.server.netty.HttpResponseBuilder;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;

@ChannelHandler.Sharable
class Http1Handler extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Http1Handler.class.getName());

    private final NettyHttpServer nettyHttpServer;

    public Http1Handler(NettyHttpServer nettyHttpServer) {
        this.nettyHttpServer = nettyHttpServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpPipelinedRequest httpPipelinedRequest) {
            try {
                if (httpPipelinedRequest.getRequest() instanceof FullHttpRequest fullHttpRequest) {
                    requestReceived(ctx, fullHttpRequest, httpPipelinedRequest.getSequenceId());
                }
            } finally {
                httpPipelinedRequest.release();
            }
        } else if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
            try {
                if (HttpUtil.is100ContinueExpected(fullHttpRequest)) {
                    DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.CONTINUE);
                    ctx.writeAndFlush(fullHttpResponse);
                    return;
                }
                requestReceived(ctx, fullHttpRequest, 0);
            } finally {
                fullHttpRequest.release();
            }
        } else {
            super.channelRead(ctx, msg);
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

    protected void requestReceived(ChannelHandlerContext ctx,
                                   FullHttpRequest fullHttpRequest,
                                   Integer sequenceId) {
        HttpAddress httpAddress = ctx.channel().attr(NettyHttpServerConfig.ATTRIBUTE_KEY_HTTP_ADDRESS).get();
        try {
            HttpResponseBuilder serverResponseBuilder = HttpResponse.builder()
                    .setChannelHandlerContext(ctx);
            if (nettyHttpServer.getNettyHttpServerConfig().isPipeliningEnabled()) {
                serverResponseBuilder.setSequenceId(sequenceId);
            }
            serverResponseBuilder.shouldClose("close".equalsIgnoreCase(fullHttpRequest.headers().get(HttpHeaderNames.CONNECTION)));
            // the base URL construction may fail with exception. In that case, we return a built-in 400 Bad Request.
            HttpRequestBuilder serverRequestBuilder = HttpRequest.builder()
                    .setFullHttpRequest(fullHttpRequest)
                    .setBaseURL(httpAddress,
                            fullHttpRequest.uri(),
                            fullHttpRequest.headers().get(HttpHeaderNames.HOST))
                    .setLocalAddress((InetSocketAddress) ctx.channel().localAddress())
                    .setRemoteAddress((InetSocketAddress) ctx.channel().remoteAddress())
                    .setSequenceId(sequenceId);
            nettyHttpServer.dispatch(serverRequestBuilder, serverResponseBuilder);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "bad request: " + e.getMessage(), e);
            DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_REQUEST);
            ctx.writeAndFlush(fullHttpResponse);
            ctx.close();
        }
    }
}
