package org.xbib.net.http.netty.client.secure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

public class AkamaiTest {

    private static final Logger logger = Logger.getLogger(AkamaiTest.class.getName());

    /**
     * Problems with akamai:
     * failing: Cannot invoke "io.netty.handler.codec.http2.AbstractHttp2StreamChannel.fireChildRead(io.netty.handler.codec.http2.Http2Frame)" because "channel" is null      * demo/h2_demo_frame.html sends no content, only a push promise, and does not continue
     *
     * @throws IOException if test fails
     */
    @Test
    void testAkamai() throws IOException {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://http2.akamai.com/demo/h2_demo_frame.html")
                    .setVersion("HTTP/2.0")
                    .setResponseListener(resp -> logger.log(Level.INFO, "got HTTP/2 response: " +
                            resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8)))
                    .build();
            client.execute(request).get().close();
        }
    }
}
