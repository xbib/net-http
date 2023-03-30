package org.xbib.net.http.server.netty.secure.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.netty.HttpResponse;
import org.xbib.net.http.server.netty.HttpResponseBuilder;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.http2.Http2Handler;
import org.xbib.net.http.server.netty.secure.HttpsRequest;
import org.xbib.net.http.server.netty.secure.HttpsRequestBuilder;
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.netty.secure.ServerNameIndicationHandler;

@ChannelHandler.Sharable
public class Https2Handler extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Http2Handler.class.getName());

    private final NettyHttpServer nettyHttpServer;

    public Https2Handler(NettyHttpServer nettyHttpServer) {
        this.nettyHttpServer = nettyHttpServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest fullHttpRequest) {
            HttpAddress httpAddress = ctx.channel().attr(NettyHttpsServerConfig.ATTRIBUTE_KEY_HTTP_ADDRESS).get();
            try {
                Integer streamId = fullHttpRequest.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
                HttpResponseBuilder httpsResponseBuilder = HttpResponse.builder()
                        .setChannelHandlerContext(ctx)
                        .setVersion(HttpVersion.HTTP_2_0);
                if (streamId != null) {
                    httpsResponseBuilder.setStreamId(streamId + 1);
                }
                HttpsRequestBuilder httpsRequestBuilder = HttpsRequest.builder()
                        .setFullHttpRequest(fullHttpRequest)
                        .setBaseURL(httpAddress,
                                fullHttpRequest.uri(),
                                fullHttpRequest.headers().get(HttpHeaderNames.HOST))
                        .setLocalAddress((InetSocketAddress) ctx.channel().localAddress())
                        .setRemoteAddress((InetSocketAddress) ctx.channel().remoteAddress())
                        .setStreamId(streamId);
                if ("PRI".equals(fullHttpRequest.method().name())) {
                    nettyHttpServer.getApplication().dispatch(httpsRequestBuilder, httpsResponseBuilder, HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED);
                    return;
                }
                httpsResponseBuilder.shouldClose("close".equalsIgnoreCase(fullHttpRequest.headers().get(HttpHeaderNames.CONNECTION)));
                ServerNameIndicationHandler serverNameIndicationHandler =
                        ctx.channel().attr(NettyHttpsServerConfig.ATTRIBUTE_KEY_SNI_HANDLER).get();
                if (serverNameIndicationHandler != null) {
                    httpsRequestBuilder.setSNIHost(serverNameIndicationHandler.hostname());
                    httpsRequestBuilder.setSSLSession(serverNameIndicationHandler.getSslHandler().engine().getSession());
                }
                nettyHttpServer.getApplication().dispatch(httpsRequestBuilder, httpsResponseBuilder);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "bad request: " + e.getMessage(), e);
                DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(io.netty.handler.codec.http.HttpVersion.valueOf(httpAddress.getVersion().text()),
                        io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST);
                ctx.writeAndFlush(fullHttpResponse);
                ctx.close();
            } finally {
                fullHttpRequest.release();
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
