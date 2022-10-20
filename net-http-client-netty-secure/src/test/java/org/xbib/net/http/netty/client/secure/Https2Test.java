package org.xbib.net.http.netty.client.secure;

import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Https2Test {

    private static final Logger logger = Logger.getLogger(Https2Test.class.getName());

    @Disabled
    @Test
    void testXbib() throws Exception {
        // the xbib server does not offer HTTP/2 so this does not work!
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://xbib.org/")
                    .setVersion("HTTP/2.0")
                    .setResponseListener(resp -> logger.log(Level.INFO, "got HTTP/2 response: " +
                            resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8)))
                    .build();
            client.execute(request).get().close();
        }
    }

    @Test
    void testGoogleFollwRedirect() throws Exception {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://google.com")
                    .setVersion("HTTP/2.0")
                    .setFollowRedirect(true) // default is true, https://www.google.com/
                    .setResponseListener(resp -> logger.log(Level.INFO, "got HTTP/2 response: " +
                            resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8)))
                    .build();
            client.execute(request).get().close();
        }
    }

    @Test
    void testHttp1WithTlsV13() throws Exception {
        AtomicBoolean success = new AtomicBoolean();
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setSecureProtocolName(new String[] { "TLSv1.3" })
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://google.com")
                    .setVersion("HTTP/2.0")
                    .setFollowRedirect(true) // default is true, https://www.google.com/
                    .setResponseListener(resp -> {
                        logger.log(Level.INFO, "got response: " +
                                resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8));
                        success.set(true);
                    })
                    .build();
            client.execute(request).get().close();
        }
        assertTrue(success.get());
    }

    @Test
    void testParallelRequestsAndClientClose() throws IOException {
        AtomicBoolean success1 = new AtomicBoolean();
        AtomicBoolean success2 = new AtomicBoolean();
        NettyHttpClientConfig config = new NettyHttpsClientConfig();
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request1 = HttpRequest.get()
                    .setURL("https://google.com")
                    .setVersion("HTTP/2.0")
                    .setFollowRedirect(true)
                    .setResponseListener(resp -> {
                        logger.log(Level.INFO, "got response1: " +
                                resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8));
                        success1.set(true);
                    })
                    .build();
            HttpRequest request2 = HttpRequest.get()
                    .setURL("https://google.com")
                    .setVersion("HTTP/2.0")
                    .setFollowRedirect(true)
                    .setResponseListener(resp -> {
                        logger.log(Level.INFO, "got response2: " +
                                resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8));
                        success2.set(true);
                    })
                    .build();
            client.execute(request1);
            client.execute(request2);
        }
        assertTrue(success1.get());
        assertTrue(success2.get());
    }
}
