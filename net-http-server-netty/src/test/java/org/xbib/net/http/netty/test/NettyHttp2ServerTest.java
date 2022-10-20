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
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.server.BaseApplication;
import org.xbib.net.http.server.BaseHttpDomain;
import org.xbib.net.http.server.BaseHttpService;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;
import org.xbib.net.http.server.route.BaseHttpRouter;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyHttp2ServerTest {

    private static final Logger logger = Logger.getLogger(NettyHttp2ServerTest.class.getName());

    @Test
    public void testHttp2() throws Exception {
        // note that h2c in cleartext is very uncommon, browser do not support this.
        URL url = URL.from("http://localhost:8008/domain");
        HttpAddress httpAddress1 = HttpAddress.http2(url);
        NettyHttpServerConfig nettyHttpServerConfig = new NettyHttpServerConfig();
        nettyHttpServerConfig.setServerName("NettyHttp2ClearTextServer",
                Bootstrap.class.getPackage().getImplementationVersion());
        nettyHttpServerConfig.setNetworkClass(NetworkClass.ANY);
        nettyHttpServerConfig.setDebug(true);
        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(nettyHttpServerConfig)
                .setApplication(BaseApplication.builder()
                    .setRouter(BaseHttpRouter.builder()
                        .addDomain(BaseHttpDomain.builder()
                                .setHttpAddress(httpAddress1)
                                .addService(BaseHttpService.builder()
                                        .setPath("/domain")
                                        .setHandler(ctx -> {
                                            ctx.response()
                                                    .setResponseStatus(HttpResponseStatus.OK)
                                                    .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                    .setCharset(StandardCharsets.UTF_8);
                                            ctx.write("domain " +
                                                    ctx.httpRequest().getParameter() + " " +
                                                    ctx.httpRequest().getLocalAddress() +  " " +
                                                    ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                            .build())
                    .build())
                .build()) {
            server.bind();
            NettyHttpClientConfig config = new NettyHttpClientConfig()
                    .setDebug(true);
            AtomicBoolean received = new AtomicBoolean();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.get()
                        .setURL(url)
                        .setVersion(HttpVersion.HTTP_2_0)
                        .setResponseListener(resp -> {
                            logger.log(Level.INFO, "got HTTP/2 response: " +
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
