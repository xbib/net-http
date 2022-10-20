package org.xbib.net.http.client.netty.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.DefaultHttp2SettingsFrame;
import io.netty.handler.codec.http2.Http2ConnectionPrefaceAndSettingsFrameWrittenEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.client.netty.Interaction;

public class Http2Messages extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(Http2Messages.class.getName());

    private final Interaction interaction;

    public Http2Messages(Interaction interaction) {
        this.interaction = interaction;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttp2SettingsFrame) {
            DefaultHttp2SettingsFrame settingsFrame = (DefaultHttp2SettingsFrame) msg;
            interaction.settingsReceived(settingsFrame.settings());
            logger.log(Level.FINEST, "received settings ");
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof Http2ConnectionPrefaceAndSettingsFrameWrittenEvent) {
            Http2ConnectionPrefaceAndSettingsFrameWrittenEvent event =
                    (Http2ConnectionPrefaceAndSettingsFrameWrittenEvent) evt;
            logger.log(Level.FINEST, "received preface and setting written event " + event);
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        interaction.fail(ctx.channel(), cause);
    }
}
