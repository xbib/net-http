package org.xbib.net.http.netty.test;

import io.netty.bootstrap.Bootstrap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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

public class NettyHttpServerFailureTest {

    private static final Logger logger = Logger.getLogger(NettyHttpServerTest.class.getName());

    @Test
    public void testBadRequest() throws Exception {
        URL url = URL.from("http://localhost:8008/domain/");
        HttpAddress httpAddress1 = HttpAddress.http1(url);
        NettyHttpServerConfig nettyHttpServerConfig = new NettyHttpServerConfig();
        nettyHttpServerConfig.setServerName("NettyHttpServer", Bootstrap.class.getPackage().getImplementationVersion());
        nettyHttpServerConfig.setNetworkClass(NetworkClass.LOCAL);
        nettyHttpServerConfig.setDebug(true);
        nettyHttpServerConfig.setPipelining(false);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress1)
                        .addService(BaseHttpService.builder()
                                .setPath("/domain")
                                .setHandler(ctx -> {
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .charset(StandardCharsets.UTF_8)
                                            .body("domain" +
                                                    " parameter = " + ctx.getRequest().getParameter().toString() +
                                                    " local address = " + ctx.getRequest().getLocalAddress() +
                                                    " remote address = " + ctx.getRequest().getRemoteAddress() +
                                                    " attributes = " + ctx.getAttributes());
                                })
                                .build())
                        .build())
                .build();

        Executor executor = BaseExecutor.builder()
                .build();

        try (NettyHttpServer server = NettyHttpServer.builder()
                .setHttpServerConfig(nettyHttpServerConfig)
                .setApplication(BaseApplication.builder()
                        .setHome(Paths.get("."))
                        .setExecutor(executor)
                        .setRouter(router)
                        .build())
                .build()) {
            server.bind();

            // bad request

            Socket socket = new Socket(InetAddress.getByName(url.getHost()), url.getPort());
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            pw.println("GET /::{} HTTP/1.1");
            pw.println("Host: " + url.getHost() + ":" + url.getPort());
            pw.println("");
            pw.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String string;
            logger.log(Level.INFO, "enter read loop"); // should print bad request
            while ((string = br.readLine()) != null) {
                logger.log(Level.INFO, string); // should print bad request
            }
            br.close();

            // good request

            NettyHttpClientConfig config = new NettyHttpClientConfig()
                    .setDebug(true);
            AtomicInteger count = new AtomicInteger();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.get()
                        .setURL(url)
                        .setResponseListener(resp -> {
                            logger.log(Level.INFO, "got response:" +
                                    " status = " + resp.getStatus() +
                                    " header = " + resp.getHeaders() +
                                    " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8));
                            count.incrementAndGet();
                        })
                        .build();
                client.execute(request).get().close();
            }
            assertEquals(1L, count.get());
        }
    }
}
