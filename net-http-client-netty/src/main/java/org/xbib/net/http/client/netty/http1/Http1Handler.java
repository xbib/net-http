package org.xbib.net.http.client.netty.http1;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.client.netty.Interaction;

@ChannelHandler.Sharable
public class Http1Handler extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Http1Handler.class.getName());

    private final Interaction interaction;

    public Http1Handler(Interaction interaction) {
        this.interaction = interaction;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse httpResponse = (FullHttpResponse) msg;
            try {
                interaction.responseReceived(ctx.channel(), null, httpResponse);
            } finally {
                httpResponse.release();
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
        interaction.fail(ctx.channel(), cause);
        ctx.close();
    }
}
