package org.xbib.net.http.server.netty;

import io.netty.channel.Channel;
import org.xbib.net.http.HttpAddress;

public interface HttpChannelInitializer {

    boolean supports(HttpAddress httpAddress);

    void init(Channel channel, NettyHttpServer server, NettyCustomizer customizer);
}
