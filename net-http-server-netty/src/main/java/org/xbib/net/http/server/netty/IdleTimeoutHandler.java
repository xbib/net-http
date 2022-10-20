package org.xbib.net.http.server.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Idle timeout handler.
 */
@ChannelHandler.Sharable
public class IdleTimeoutHandler extends IdleStateHandler {

    private static final Logger logger = Logger.getLogger(IdleTimeoutHandler.class.getName());

    public IdleTimeoutHandler(int idleTimeoutMillis) {
        super(idleTimeoutMillis, idleTimeoutMillis, idleTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        if (!evt.isFirst()) {
            return;
        }
        logger.log(Level.FINER, () -> "closing an idle connection " + ctx.channel());
        ctx.close();
    }
}
