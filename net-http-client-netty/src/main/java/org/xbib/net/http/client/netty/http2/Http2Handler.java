package org.xbib.net.http.client.netty.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.client.netty.Interaction;

@ChannelHandler.Sharable
public class Http2Handler extends ChannelDuplexHandler {

    private static final Logger logger = Logger.getLogger(Http2Handler.class.getName());

    private final Interaction interaction;

    public Http2Handler(Interaction interaction) {
        this.interaction = interaction;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse httpResponse = (FullHttpResponse) msg;
            try {
                Integer streamId = httpResponse.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
                interaction.responseReceived(ctx.channel(), streamId, httpResponse);
            } finally {
                httpResponse.release();
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        interaction.inactive(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.FINE, "exception caught");
        interaction.fail(ctx.channel(), cause);
        ctx.close();
    }
}
