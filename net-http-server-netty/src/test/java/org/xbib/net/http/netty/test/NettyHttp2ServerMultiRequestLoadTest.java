package org.xbib.net.http.netty.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.ResourceLeakDetector;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.server.application.BaseApplication;
import org.xbib.net.http.server.domain.BaseHttpDomain;
import org.xbib.net.http.server.service.BaseHttpService;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.route.BaseHttpRouter;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NettyHttp2ServerMultiRequestLoadTest {

    private static final Logger logger = Logger.getLogger(NettyHttp2ServerMultiRequestLoadTest.class.getName());

    @Test
    public void testHttp2Load() throws Exception {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        URL url = URL.from("http://localhost:8008/domain");
        HttpAddress httpAddress = HttpAddress.http2(url);
        NettyHttpServerConfig serverConfig = new NettyHttpServerConfig();
        serverConfig.setServerName("NettyHttp2CleartextServer", Bootstrap.class.getPackage().getImplementationVersion());
        serverConfig.setNetworkClass(NetworkClass.LOOPBACK);
        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(serverConfig)
                .setApplication(BaseApplication.builder()
                    .setRouter(BaseHttpRouter.builder()
                        .addDomain(BaseHttpDomain.builder()
                                .setHttpAddress(httpAddress)
                                .addService(BaseHttpService.builder()
                                        .setPath("/domain")
                                        .setHandler(ctx -> {
                                            ctx.response()
                                                    .setResponseStatus(HttpResponseStatus.OK)
                                                    .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                    .setCharset(StandardCharsets.UTF_8);
                                            ctx.write("domain: " +
                                                    " base URL = " + ctx.httpRequest().getBaseURL() +
                                                    " parameter = " + ctx.httpRequest().getParameter().allToString() +
                                                    " attributes = " + ctx.getAttributes() +
                                                    " local address = " + ctx.httpRequest().getLocalAddress() +
                                                    " remote address = " + ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                        .build())
                    .build())
                .build()) {
            server.bind();
            NettyHttpClientConfig config = new NettyHttpClientConfig();
            int requests = 1024;
            AtomicInteger count = new AtomicInteger();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                for (int i = 0; i < requests; i++) {
                    HttpRequest request = HttpRequest.get()
                            .setVersion(HttpVersion.HTTP_2_0)
                            .setURL(url)
                            .setResponseListener(resp -> {
                                logger.log(Level.INFO, "got response " +
                                        " status = " + resp.getStatus() +
                                        " header = " + resp.getHeaders() +
                                        " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8));
                                count.incrementAndGet();
                            })
                            .setExceptionListener(e -> {
                                logger.log(Level.SEVERE, e.getMessage(), e);
                            })
                            .setTimeoutListener(listener -> {
                                logger.log(Level.SEVERE, "timeout");
                            }, 5000L)
                            .build();
                    client.execute(request).get(); // we need a get here
                }
            }
            logger.log(Level.INFO, "count = " + count.get());
            assertEquals(requests, count.get());
        }
    }
}
