package org.xbib.net.http.netty.test;

import io.netty.bootstrap.Bootstrap;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.Parameter;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpMethod;
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyHttpServerByteOrderMarkTest {

    private static final Logger logger = Logger.getLogger(NettyHttpServerByteOrderMarkTest.class.getName());

    @Test
    void testJsonPostRequest() throws Exception {

        String body = "{\"syntax\":\"CQL\",\"offset\":0,\"size\":10,\"service\":[\"hbz\"],\"op\":[\"\",\"@and\",\"@and\"],\"key\":[\"bib.any\",\"bib.title\",\"bib.identifierISSN\"],\"query\":[\"linux einführung\",\"\",\"\"]}";

        URL url = URL.from("http://localhost:8008/");
        HttpAddress httpAddress1 = HttpAddress.http1(url);
        NettyHttpServerConfig nettyHttpServerConfig = new NettyHttpServerConfig();
        nettyHttpServerConfig.setServerName("NettyHttpServer",
                Bootstrap.class.getPackage().getImplementationVersion());
        nettyHttpServerConfig.setNetworkClass(NetworkClass.LOCAL);
        nettyHttpServerConfig.setDebug(true);
        nettyHttpServerConfig.setChunkWriteEnabled(true);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress1)
                        .addService(BaseHttpService.builder()
                                .setPath("/")
                                .setMethod(HttpMethod.POST)
                                .setHandler(ctx -> {
                                    logger.log(Level.FINEST, "handler starting");
                                    String content = ctx.getRequestBuilder().getBodyAsChars(StandardCharsets.UTF_8).toString();
                                    logger.log(Level.FINEST, "got content = " + content);
                                    logger.log(Level.FINEST, "got FORM params op = " + ctx.getRequest().getParameter().getAll("op", Parameter.Domain.FORM));
                                    logger.log(Level.FINEST, "got FORM params key = " + ctx.getRequest().getParameter().getAll("key", Parameter.Domain.FORM));
                                    logger.log(Level.FINEST, "got FORM params query = " + ctx.getRequest().getParameter().getAll("query", Parameter.Domain.FORM));
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .charset(StandardCharsets.UTF_8)
                                            .body("parameter = " + ctx.getRequest().getParameter().allToString() +
                                            " local address = " + ctx.getRequest().getLocalAddress() +
                                            " remote address = " + ctx.getRequest().getRemoteAddress() +
                                            " attributes = " + ctx.getAttributes() +
                                            " content = " + content);
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
                    .setGzipEnabled(true)
                    .setChunkWriteEnabled(true)
                    .setObjectAggregationEnabled(true)
                    .setDebug(true);
            AtomicBoolean received = new AtomicBoolean();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.post()
                        .setURL(url)
                        .content(body, "application/json", StandardCharsets.UTF_8)
                        .setResponseListener(resp -> {
                            logger.log(Level.INFO, " status = " + resp.getStatus() +
                                    " got response headers = " + resp.getHeaders() +
                                    " got response body = " + resp.getBodyAsChars(StandardCharsets.UTF_8));
                            received.set(true);
                        })
                        .build();
                client.execute(request).get().close();
            }

            assertTrue(received.get());
        }
    }
}
