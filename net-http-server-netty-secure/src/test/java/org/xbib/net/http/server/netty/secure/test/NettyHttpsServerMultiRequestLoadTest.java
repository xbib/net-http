package org.xbib.net.http.server.netty.secure.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.ResourceLeakDetector;
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NettyHttpsServerMultiRequestLoadTest {

    private static final Logger logger = Logger.getLogger(NettyHttpsServerMultiRequestLoadTest.class.getName());

    @Test
    public void testHttps1Load() throws Exception {
        // client HTTP 1.1, server HTTP 1.1 (no upgrade)
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        URL url = URL.from("https://localhost:8443/secure");
        HttpsAddress httpsAddress = HttpsAddress.builder()
                .setVersion(HttpVersion.HTTP_1_1)
                .setSecure(true)
                .setHost(url.getHost())
                .setPort(url.getPort())
                .setSelfCert(url.getHost())
                .build();
        NettyHttpsServerConfig serverConfig = new NettyHttpsServerConfig();
        serverConfig.setServerName("NettySecureHttpServer", Bootstrap.class.getPackage().getImplementationVersion());
        serverConfig.setNetworkClass(NetworkClass.LOOPBACK);
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
                                        .setHandler(ctx -> {
                                            logger.log(Level.INFO, "executing /secure");
                                            ctx.response()
                                                    .setResponseStatus(HttpResponseStatus.OK)
                                                    .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                    .setCharset(StandardCharsets.UTF_8);
                                            ctx.write("secure domain: " +
                                                    " SNI host = " + ctx.httpRequest().as(HttpsRequest.class).getSNIHost() +
                                                    " SSL peer host = " + ctx.httpRequest().as(HttpsRequest.class).getSSLSession() +
                                                    " base URL = " + ctx.httpRequest().getBaseURL() +
                                                    " parameter = " + ctx.httpRequest().getParameter().allToString() +
                                                    " attributes = " + ctx.attributes() +
                                                    " local address = " + ctx.httpRequest().getLocalAddress() +
                                                    " remote address = " + ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                        .build())
                    .build())
                .build()) {
            server.bind();
            NettyHttpClientConfig config = new NettyHttpsClientConfig()
                    .setSecureProtocolName(new String[]{ "TLSv1.2" } )
                    .trustInsecure();
            int requests = 1024;
            AtomicInteger count = new AtomicInteger();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                for (int i = 0; i < requests; i++) {
                    HttpRequest request = HttpRequest.get()
                            .setVersion(HttpVersion.HTTP_1_1)
                            .setURL(url)
                            .setResponseListener(resp -> {
                                logger.log(Level.INFO, "got response (HTTPS 1.1): " +
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

    @Test
    public void testHttps1LoadUpgrade() throws Exception {
        // client HTTP 1.1, server HTTP 2.0 (upgrade) - use this with browsers
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
                                        .setHandler(ctx -> {
                                            ctx.response()
                                                .setResponseStatus(HttpResponseStatus.OK)
                                                .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                .setCharset(StandardCharsets.UTF_8);
                                            ctx.write("secure domain: " +
                                                    " SNI host = " + ctx.httpRequest().as(HttpsRequest.class).getSNIHost() +
                                                    " SSL session = " + ctx.httpRequest().as(HttpsRequest.class).getSSLSession() +
                                                    " base URL = " + ctx.httpRequest().getBaseURL() +
                                                    " parameter = " + ctx.httpRequest().getParameter().allToString() +
                                                    " attributes = " + ctx.attributes() +
                                                    " local address = " + ctx.httpRequest().getLocalAddress() +
                                                    " remote address = " + ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                        .build())
                    .build())
                .build()) {
            server.bind();
            NettyHttpClientConfig config = new NettyHttpsClientConfig()
                    .trustInsecure();
            int requests = 1024;
            AtomicInteger count = new AtomicInteger();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                for (int i = 0; i < requests; i++) {
                    HttpRequest request = HttpRequest.get()
                            .setURL(url)
                            .setVersion(HttpVersion.HTTP_1_1)
                            .setResponseListener(resp -> {
                                logger.log(Level.INFO, "got response (HTTPS 1.1 client -> HTTPS 2.0) server: " +
                                        " status = " + resp.getStatus() +
                                        " header = " + resp.getHeaders() +
                                        " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8));
                                count.incrementAndGet();
                            })
                            .build();
                    client.execute(request).get();
                }
            }
            logger.log(Level.INFO, "count = " + count.get());
            assertEquals(requests, count.get());
        }
    }
}
