package org.xbib.net.http.netty.conscrypt;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

public class Http1Test {

    private static final Logger logger = Logger.getLogger(Http1Test.class.getName());

    @Test
    void testGoogleConscrypt() throws Exception {

        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setSecureSocketProviderName("CONSCRYPT")
                .setDebug(true);

        //  java.security.cert.CertificateException: Unknown authType: GENERIC
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://www.google.de/")
                    .setResponseListener(resp -> logger.log(Level.INFO,
                            "got response: " + resp.getHeaders() +
                                    resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                    " status=" + resp.getStatus()))
                    .build();
            logger.log(Level.INFO, "request = " + request);
            client.execute(request).get().close();
        }
    }
}
