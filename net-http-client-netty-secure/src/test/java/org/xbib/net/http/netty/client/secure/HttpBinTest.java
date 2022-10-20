package org.xbib.net.http.netty.client.secure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;
import org.xbib.net.http.cookie.Cookie;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpBinTest {

    private static final Logger logger = Logger.getLogger(HttpBinTest.class.getName());

    /**
     * Test httpbin.org "Set-Cookie:" header after redirection of URL.
     *
     * The reponse body should be
     * <pre>
     *   {
     *     "cookies": {
     *       "name": "value"
     *     }
     *   }
     * </pre>
     * @throws IOException if test fails
     */
    @Test
    void testHttpBinCookies() throws IOException {
        AtomicBoolean success = new AtomicBoolean();
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("http://httpbin.org/cookies/set?name=value")
                    .setResponseListener(resp -> {
                        logger.log(Level.INFO, "got HTTP/2 response: " +
                                resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8));
                        for (Cookie cookie : resp.getCookies()) {
                            logger.log(Level.INFO, "got cookie: " + cookie.toString());
                            if ("name".equals(cookie.name()) && ("value".equals(cookie.value()))) {
                                success.set(true);
                            }
                        }
                    })
                    .build();
            client.execute(request).get().close();
        }
        assertTrue(success.get());
    }
}
