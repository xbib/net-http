package org.xbib.net.http.client.netty;

import io.netty.channel.Channel;
import java.io.Closeable;
import java.io.IOException;
import org.xbib.net.http.HttpAddress;

public interface HttpChannelInitializer {

    boolean supports(HttpAddress httpAddress);

    Interaction newInteraction(NettyHttpClient client, HttpAddress httpAdress);

    void init(Channel channel,
              HttpAddress httpAddress,
              NettyHttpClient client,
              NettyCustomizer nettyCustomizer,
              Interaction interaction) throws IOException;

}
