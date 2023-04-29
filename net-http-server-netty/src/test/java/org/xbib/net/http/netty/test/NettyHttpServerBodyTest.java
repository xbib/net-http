package org.xbib.net.http.netty.test;

import io.netty.bootstrap.Bootstrap;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.server.application.BaseApplication;
import org.xbib.net.http.server.domain.BaseHttpDomain;
import org.xbib.net.http.server.executor.BaseExecutor;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.route.BaseHttpRouter;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.http.server.service.BaseHttpService;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyHttpServerBodyTest {

    private static final Logger logger = Logger.getLogger(NettyHttpServerBodyTest.class.getName());

    @Test
    public void testHttp() throws Exception {
        URL url = URL.from("http://localhost:8008/domain/");
        HttpAddress httpAddress1 = HttpAddress.http1(url);
        NettyHttpServerConfig nettyHttpServerConfig = new NettyHttpServerConfig();
        nettyHttpServerConfig.setServerName("NettyHttpServer",
                Bootstrap.class.getPackage().getImplementationVersion());
        nettyHttpServerConfig.setNetworkClass(NetworkClass.LOCAL);
        nettyHttpServerConfig.setDebug(true);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress1)
                        .addService(BaseHttpService.builder()
                                .setPath("/domain")
                                .setHandler(ctx -> {
                                    String body = ctx.getRequestBuilder().getBodyAsChars(StandardCharsets.UTF_8).toString();
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .charset(StandardCharsets.UTF_8)
                                            .body("parameter = " + ctx.getRequest().getParameter().allToString() +
                                            " local address = " + ctx.getRequest().getLocalAddress() +
                                            " remote address = " + ctx.getRequest().getRemoteAddress() +
                                            " attributes = " + ctx.getAttributes() +
                                            " body = " + body)
                                            .done();
                                })
                                .build())
                        .build())
                .build();

        Executor executor = BaseExecutor.builder()
                .build();

        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(nettyHttpServerConfig)
                .setApplication(BaseApplication.builder()
                        .setExecutor(executor)
                        .setRouter(router)
                  .build())
                .build()) {
            server.bind();
            NettyHttpClientConfig config = new NettyHttpClientConfig()
                    .setDebug(true);
            AtomicBoolean received = new AtomicBoolean();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.post()
                        .setURL(url)
                        .content("Hello, I'm a simple POST body for Jörg",
                                "text/plain",
                                StandardCharsets.UTF_8)
                        .setResponseListener(resp -> {
                            logger.log(Level.INFO, "got response:" +
                                    " status = " + resp.getStatus() +
                                    " header = " + resp.getHeaders() +
                                    " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8));
                            received.set(true);
                        })
                        .build();
                client.execute(request).get().close();
            }
            assertTrue(received.get());
        }
    }
}
