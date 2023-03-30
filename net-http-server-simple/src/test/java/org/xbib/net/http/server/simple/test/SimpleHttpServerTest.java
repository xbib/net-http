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
import org.xbib.net.http.server.route.BaseHttpRouter;
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
        SimpleHttpServer server = SimpleHttpServer.builder()
                .setHttpServerConfig(new HttpServerConfig()
                        .setServerName("SimpleHttpServer", SimpleHttpServer.class.getPackage().getImplementationVendor())
                        .setNetworkClass(NetworkClass.SITE)
                )
                .setApplication(BaseApplication.builder()
                    .setRouter(BaseHttpRouter.builder()
                        .addDomain(BaseHttpDomain.builder()
                                .setHttpAddress(httpAddress1)
                                .addService(BaseHttpService.builder()
                                        .setPath("/domain1")
                                        .setHandler(ctx -> {
                                                ctx.response()
                                                        .setResponseStatus(HttpResponseStatus.OK)
                                                        .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                        .setCharset(StandardCharsets.UTF_8);
                                                ctx.write("domain1 " +
                                                        ctx.httpRequest().getParameter() + " " +
                                                        ctx.httpRequest().getLocalAddress() +  " " +
                                                        ctx.httpRequest().getRemoteAddress());
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
                                                ctx.response()
                                                        .setResponseStatus(HttpResponseStatus.OK)
                                                        .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                        .setCharset(StandardCharsets.UTF_8);
                                                ctx.write("domain2 " +
                                                        ctx.httpRequest().getParameter() + " " +
                                                        ctx.httpRequest().getLocalAddress() +  " " +
                                                        ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                        .build())
                    .build())
                .build();
        server.bind();
        server.loop();
    }
}
