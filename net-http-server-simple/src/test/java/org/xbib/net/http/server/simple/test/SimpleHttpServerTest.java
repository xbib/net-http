package org.xbib.net.http.server.simple.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.application.BaseApplication;
import org.xbib.net.http.server.domain.BaseHttpDomain;
import org.xbib.net.http.server.executor.BaseExecutor;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.route.BaseHttpRouter;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.http.server.service.BaseHttpService;
import org.xbib.net.http.server.HttpServerConfig;
import org.xbib.net.http.server.resource.FileResourceHandler;
import org.xbib.net.http.server.simple.SimpleHttpServer;

import java.nio.charset.StandardCharsets;

public class SimpleHttpServerTest {

    @Disabled
    @Test
    public void simpleServerTest() throws Exception {
        HttpAddress httpAddress1 = HttpAddress.http1("localhost", 8008);
        HttpAddress httpAddress2 = HttpAddress.http1("localhost", 8008);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress1)
                        .addService(BaseHttpService.builder()
                                .setPath("/domain1")
                                .setHandler(ctx -> {
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .charset(StandardCharsets.UTF_8)
                                            .body("domain1 " +
                                            ctx.getRequest().getParameter() + " " +
                                            ctx.getRequest().getLocalAddress() +  " " +
                                            ctx.getRequest().getRemoteAddress());
                                })
                                .build())
                        .addService(BaseHttpService.builder()
                                .setPath("/file1/*")
                                .setHandler(new FileResourceHandler())
                                .build())
                        .build())
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress2)
                        .addService(BaseHttpService.builder()
                                .setPath("/domain2")
                                .setHandler(ctx -> {
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .charset(StandardCharsets.UTF_8)
                                            .body("domain2 " +
                                            ctx.getRequest().getParameter() + " " +
                                            ctx.getRequest().getLocalAddress() +  " " +
                                            ctx.getRequest().getRemoteAddress());
                                })
                                .build())
                        .build())
                .build();

        Executor executor = BaseExecutor.builder()
                .build();

        SimpleHttpServer server = SimpleHttpServer.builder()
                .setHttpServerConfig(new HttpServerConfig()
                        .setServerName("SimpleHttpServer", SimpleHttpServer.class.getPackage().getImplementationVendor())
                        .setNetworkClass(NetworkClass.SITE))
                .setApplication(BaseApplication.builder()
                        .setExecutor(executor)
                        .setRouter(router)
                    .build())
                .build();
        server.bind();
        server.loop();
    }
}
