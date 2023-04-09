package org.xbib.net.http.netty.test;

import io.netty.bootstrap.Bootstrap;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.client.Message;
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

public class NettyHttpServerFileUploadTest {

    private static final Logger logger = Logger.getLogger(NettyHttpServerFileUploadTest.class.getName());

    @Test
    public void testSimpleFileUpload() throws Exception {
        URL url = URL.from("http://localhost:8008/");
        HttpAddress httpAddress1 = HttpAddress.http1(url);
        NettyHttpServerConfig nettyHttpServerConfig = new NettyHttpServerConfig();
        nettyHttpServerConfig.setServerName("NettyHttpServer",
                Bootstrap.class.getPackage().getImplementationVersion());
        nettyHttpServerConfig.setNetworkClass(NetworkClass.LOCAL);
        nettyHttpServerConfig.setDebug(true);
        nettyHttpServerConfig.setChunkWriteEnabled(true);
        nettyHttpServerConfig.setFileUploadEnabled(true);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress1)
                        .addService(BaseHttpService.builder()
                                .setPath("/")
                                .setMethod(HttpMethod.POST)
                                .setHandler(ctx -> {
                                    logger.log(Level.FINEST, "handler starting");
                                    String message = ctx.httpRequest().getMessages().stream()
                                            .map(m -> StandardCharsets.UTF_8.decode(m.getByteBuffer()))
                                            .collect(Collectors.joining());

                                    ctx.response()
                                            .setResponseStatus(HttpResponseStatus.OK)
                                            .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .setCharset(StandardCharsets.UTF_8);
                                    ctx.write("parameter = " + ctx.httpRequest().getParameter().allToString() +
                                            " local address = " + ctx.httpRequest().getLocalAddress() +
                                            " remote address = " + ctx.httpRequest().getRemoteAddress() +
                                            " attributes = " + ctx.getAttributes() +
                                            " message = " + message
                                    );
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
                    .setFileUploadEnabled(true)
                    .setDebug(true);
            AtomicBoolean received = new AtomicBoolean();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.post()
                        .setURL(url)
                        .addMessage(new Message("text/plain",
                                "base64",
                                "test", Paths.get("build.gradle"),
                                StandardCharsets.UTF_8))
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

    @Disabled
    @Test
    public void testLargeFileUpload() throws Exception {
        URL url = URL.from("http://localhost:8008/");
        HttpAddress httpAddress1 = HttpAddress.http1(url);
        NettyHttpServerConfig nettyHttpServerConfig = new NettyHttpServerConfig();
        nettyHttpServerConfig.setServerName("NettyHttpServer",
                Bootstrap.class.getPackage().getImplementationVersion());
        nettyHttpServerConfig.setNetworkClass(NetworkClass.LOCAL);
        nettyHttpServerConfig.setDebug(false);
        nettyHttpServerConfig.setChunkWriteEnabled(true);
        nettyHttpServerConfig.setFileUploadEnabled(true);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress1)
                        .addService(BaseHttpService.builder()
                                .setPath("/")
                                .setMethod(HttpMethod.POST)
                                .setHandler(ctx -> {
                                    List<org.xbib.net.http.server.Message> messages = ctx.httpRequest().getMessages();
                                    for (org.xbib.net.http.server.Message message : messages) {
                                        if (message.getPath() != null) {
                                            try (InputStream inputStream = Files.newInputStream(message.getPath());
                                                 OutputStream outputStream = Files.newOutputStream(Paths.get("build/" + message.getName()))) {
                                                inputStream.transferTo(outputStream);
                                            }
                                        }
                                    }
                                    ctx.response()
                                            .setResponseStatus(HttpResponseStatus.OK)
                                            .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .setCharset(StandardCharsets.UTF_8);
                                    ctx.write("parameter = " + ctx.httpRequest().getParameter().allToString() +
                                            " local address = " + ctx.httpRequest().getLocalAddress() +
                                            " remote address = " + ctx.httpRequest().getRemoteAddress() +
                                            " attributes = " + ctx.getAttributes() +
                                            " parts = " + messages.size()
                                    );
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
                    .setFileUploadEnabled(true)
                    .setDebug(false);
            AtomicBoolean received = new AtomicBoolean();
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.post()
                        .setURL(url)
                        .addMessage(new Message("application/pdf",
                                "base64",
                                "test.pdf",
                                Paths.get("/home/joerg/3904846.pdf"),
                                StandardCharsets.US_ASCII))
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
