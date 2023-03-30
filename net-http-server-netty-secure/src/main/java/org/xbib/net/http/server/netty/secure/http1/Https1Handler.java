package org.xbib.net.http.server.netty.secure.http1;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.HttpResponse;
import org.xbib.net.http.server.netty.HttpResponseBuilder;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.http1.HttpPipelinedRequest;
import org.xbib.net.http.server.netty.secure.HttpsRequest;
import org.xbib.net.http.server.netty.secure.HttpsRequestBuilder;
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.netty.secure.ServerNameIndicationHandler;

@ChannelHandler.Sharable
public class Https1Handler extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Https1Handler.class.getName());

    private final NettyHttpServer nettyHttpServer;

    public Https1Handler(NettyHttpServer nettyHttpServer) {
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
        } else if (msg instanceof FullHttpRequest fullHttpRequest) {
            try {
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

    protected void requestReceived(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, Integer sequenceId) {
        HttpAddress httpAddress = ctx.channel().attr(NettyHttpsServerConfig.ATTRIBUTE_KEY_HTTP_ADDRESS).get();
        try {
            HttpResponseBuilder serverResponseBuilder = HttpResponse.builder()
                    .setChannelHandlerContext(ctx);
            if (nettyHttpServer.getNettyHttpServerConfig().isPipeliningEnabled()) {
                serverResponseBuilder.setSequenceId(sequenceId);
            }
            // host header present? RFC2616#14.23: missing Host header gets 400
            HttpsRequestBuilder serverRequestBuilder = HttpsRequest.builder()
                    .setLocalAddress((InetSocketAddress) ctx.channel().localAddress())
                    .setRemoteAddress((InetSocketAddress) ctx.channel().remoteAddress())
                    .setSequenceId(sequenceId)
                    .setFullHttpRequest(fullHttpRequest)
                    .setBaseURL(httpAddress,
                            fullHttpRequest.uri(),
                            fullHttpRequest.headers().get(HttpHeaderNames.HOST));
            serverResponseBuilder.shouldClose("close".equalsIgnoreCase(fullHttpRequest.headers().get(HttpHeaderNames.CONNECTION)));
            // find SSL session, we have to look up the SSL handler in the SNI handler
            ServerNameIndicationHandler serverNameIndicationHandler =
                    ctx.channel().attr(NettyHttpsServerConfig.ATTRIBUTE_KEY_SNI_HANDLER).get();
            if (serverNameIndicationHandler != null) {
                serverRequestBuilder.setSNIHost(serverNameIndicationHandler.hostname());
                serverRequestBuilder.setSSLSession(serverNameIndicationHandler.getSslHandler().engine().getSession());
            }
            nettyHttpServer.getApplication().dispatch(serverRequestBuilder, serverResponseBuilder);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "bad request: " + e.getMessage(), e);
            DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.valueOf(httpAddress.getVersion().text()),
                    HttpResponseStatus.BAD_REQUEST);
            ctx.writeAndFlush(fullHttpResponse);
            ctx.close();
        }
    }
}
