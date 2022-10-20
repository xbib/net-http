package org.xbib.net.http.server.netty.secure.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.ResourceLeakDetector;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;
import org.xbib.net.http.server.BaseApplication;
import org.xbib.net.http.server.BaseHttpDomain;
import org.xbib.net.http.server.BaseHttpService;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.secure.HttpsAddress;
import org.xbib.net.http.server.netty.secure.HttpsRequest;
import org.xbib.net.http.server.netty.secure.NettyHttpsServerConfig;
import org.xbib.net.http.server.route.BaseHttpRouter;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyHttps2ServerTest {

    private static final Logger logger = Logger.getLogger(NettyHttps2ServerTest.class.getName());

    @Test
    public void testHttps2() throws Exception {
        // client HTTP 2.0 + server HTTP 2.0 (no upgrade)
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        URL url = URL.from("https://localhost:8443/secure");
        HttpsAddress httpsAddress = HttpsAddress.builder()
                .setSecure(true)
                .setVersion(HttpVersion.HTTP_2_0)
                .setHost(url.getHost())
                .setPort(url.getPort())
                .setSelfCert(url.getHost())
                .build();
        NettyHttpsServerConfig serverConfig = new NettyHttpsServerConfig();
        serverConfig.setServerName("NettySecureHttpServer", Bootstrap.class.getPackage().getImplementationVersion());
        serverConfig.setNetworkClass(NetworkClass.LOOPBACK);
        serverConfig.setDebug(true);
        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(serverConfig)
                .setApplication(BaseApplication.builder()
                    .setRouter(BaseHttpRouter.builder()
                        .addDomain(BaseHttpDomain.builder()
                                .setHttpAddress(httpsAddress)
                                .addService(BaseHttpService.builder()
                                        .setPath("/favicon.ico")
                                        .setHandler(ctx -> ctx.response()
                                                .setResponseStatus(HttpResponseStatus.NOT_FOUND)
                                                .build()
                                                .flush())
                                        .build())
                                .addService(BaseHttpService.builder()
                                        .setPath("/secure")
                                        .setHandler(ctx -> { ctx.response()
                                                .setResponseStatus(HttpResponseStatus.OK)
                                                .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                .setCharset(StandardCharsets.UTF_8);
                                                ctx.write("secure domain " +
                                                        " SNI host " + ctx.httpRequest().as(HttpsRequest.class).getSNIHost() + " " +
                                                        " SSL peer host " + ctx.httpRequest().as(HttpsRequest.class).getSSLSession() + " " +
                                                        " base URL = " + ctx.request().getBaseURL() + " " +
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
            AtomicBoolean received = new AtomicBoolean();
            NettyHttpClientConfig config = new NettyHttpsClientConfig()
                    .trustInsecure()
                    .setDebug(true);
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.get()
                        .setURL(url)
                        .setVersion(HttpVersion.HTTP_2_0)
                        .setResponseListener(resp -> {
                            logger.log(Level.INFO, "got response (HTTP/2):" +
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
