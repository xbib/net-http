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
import org.xbib.net.http.server.executor.BaseExecutor;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.http.server.service.BaseHttpService;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.route.BaseHttpRouter;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NettyHttpServerMultiRequestLoadTest {

    private static final Logger logger = Logger.getLogger(NettyHttpServerMultiRequestLoadTest.class.getName());

    @Test
    public void testHttp1Multi() throws Exception {

        int requests = 1024;
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        URL url = URL.from("http://localhost:8008/domain");
        HttpAddress httpAddress = HttpAddress.http1(url);
        NettyHttpServerConfig serverConfig = new NettyHttpServerConfig();
        serverConfig.setServerName("NettyHttpServer", Bootstrap.class.getPackage().getImplementationVersion());
        serverConfig.setNetworkClass(NetworkClass.LOCAL);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress)
                        .addService(BaseHttpService.builder()
                                .setPath("/domain")
                                .setHandler(ctx -> {
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .charset(StandardCharsets.UTF_8)
                                            .body("domain: " +
                                            " base URL = " + ctx.getRequest().getBaseURL() +
                                            " parameter = " + ctx.getRequest().getParameter().toString() +
                                            " attributes = " + ctx.getAttributes() +
                                            " local address = " + ctx.getRequest().getLocalAddress() +
                                            " remote address = " + ctx.getRequest().getRemoteAddress())
                                            .done();
                                })
                                .build())
                        .build())
                .build();

        Executor executor = BaseExecutor.builder()
                .build();

        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(serverConfig)
                .setApplication(BaseApplication.builder()
                        .setExecutor(executor)
                        .setRouter(router)
                    .build())
                .build()) {
            server.bind();
            NettyHttpClientConfig config = new NettyHttpClientConfig();
            AtomicInteger count = new AtomicInteger();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                for (int i = 0; i < requests; i++) {
                    HttpRequest request = HttpRequest.get()
                            .setVersion(HttpVersion.HTTP_1_1)
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
                    client.execute(request).get();
                }
            }
            logger.log(Level.INFO, "count = " + count.get());
            assertEquals(requests, count.get());
        }
    }
}
