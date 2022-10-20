package org.xbib.net.http.server.netty.secure.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2SettingsFrame;
import java.util.logging.Level;
import java.util.logging.Logger;

@ChannelHandler.Sharable
public class Https2Messages extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Https2Messages.class.getName());

    public Https2Messages() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DefaultHttp2SettingsFrame) {
            DefaultHttp2SettingsFrame http2SettingsFrame = (DefaultHttp2SettingsFrame) msg;
            logger.log(Level.FINER, "settings received, ignoring " + http2SettingsFrame);
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
