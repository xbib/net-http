package org.xbib.net.http.client.netty;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Http1Test {

    private static final Logger logger = Logger.getLogger(Http1Test.class.getName());

    @Test
    void testHttpGetRequest() throws Exception {
        NettyHttpClientConfig config = new NettyHttpClientConfig()
                .setDebug(true);
        AtomicBoolean received = new AtomicBoolean();
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                .setURL("http://httpbin.org")
                .setResponseListener(resp -> {
                        logger.log(Level.INFO,
                                "local address = " + resp.getLocalAddress() +
                                " got response = " + resp.getHeaders() +
                                        resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                        " status=" + resp.getStatus());
                        received.set(true);
                })
                .build();
            client.execute(request).get().close();
        }
        assertTrue(received.get());
    }
}
