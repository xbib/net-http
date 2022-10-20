package org.xbib.net.http.server.netty.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2SettingsFrame;
import java.util.logging.Level;
import java.util.logging.Logger;

@ChannelHandler.Sharable
public class Http2Messages extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Http2Messages.class.getName());

    public Http2Messages() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DefaultHttp2SettingsFrame) {
            DefaultHttp2SettingsFrame http2SettingsFrame = (DefaultHttp2SettingsFrame) msg;
            logger.log(Level.FINER, "settings received, ignoring");
        } else if (msg instanceof DefaultHttpRequest) {
            // somehow we got a HTTP 1.1 request, send "version not supported" to HTTP 1.1 client
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED);
            ctx.writeAndFlush(response);
            ctx.close();
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
