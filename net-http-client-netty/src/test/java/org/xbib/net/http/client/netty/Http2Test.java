package org.xbib.net.http.client.netty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbib.net.http.HttpVersion;

class Http2Test {

    private static final Logger logger = Logger.getLogger(Http2Test.class.getName());

    /**
     * HTTP/2 cleartext is not support by many servers.
     * This will return HTTP/1.1 Bad request and we run into a timeout.
     */
    @Test
    void testCleartext() {
        Assertions.assertThrows(IOException.class, () -> {
            NettyHttpClientConfig config = new NettyHttpClientConfig()
                    .setDebug(true);
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.get()
                        .setURL("http://httpbin.org")
                        .setVersion(HttpVersion.HTTP_2_0)
                        .setResponseListener(resp -> {
                            logger.log(Level.INFO,
                                    "local address = " + resp.getLocalAddress() +
                                            " got respons =: " + resp.getHeaders() +
                                            resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                            " status=" + resp.getStatus());
                        })
                        .build();
                client.execute(request).get().close();
            }
        });
    }
}
