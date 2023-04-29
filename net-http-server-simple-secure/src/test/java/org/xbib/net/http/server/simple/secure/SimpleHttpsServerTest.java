package org.xbib.net.http.server.simple.secure;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
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
import org.xbib.net.http.server.simple.SimpleHttpServer;

import java.nio.charset.StandardCharsets;

public class SimpleHttpsServerTest {

    private static final Logger logger = Logger.getLogger(SimpleHttpsServerTest.class.getName());

    @Test
    public void simpleSecureHttpsServerTest() throws Exception {
        HttpsAddress httpsAddress = HttpsAddress.builder()
                .setSecure(true)
                .setHost("localhost")
                .setPort(8443)
                .setSelfCert("localhost")
                .build();
        logger.log(Level.INFO, "SSL context = " + httpsAddress.getSslContext());
        HttpServerConfig serverConfig = new HttpServerConfig();
        serverConfig.setServerName("SimpleSecureHttpServer", SimpleHttpServer.class.getPackage().getImplementationVersion());
        serverConfig.setNetworkClass(NetworkClass.LOOPBACK);
        serverConfig.setDebug(true);

        HttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpsAddress)
                        .addService(BaseHttpService.builder()
                                .setPath("/favicon.ico")
                                .setHandler(ctx -> ctx.status(HttpResponseStatus.NOT_FOUND))
                                .build())
                        .addService(BaseHttpService.builder()
                                .setPath("/secure")
                                .setHandler(ctx -> {
                                    ctx.status(HttpResponseStatus.OK)
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .charset(StandardCharsets.UTF_8)
                                            .body("secure domain: " +
                                            " SNI host = " + ctx.getRequest().as(HttpsRequest.class).getSNIHost() +
                                            " SSL peer host = " + ctx.getRequest().as(HttpsRequest.class).getSSLSession() +
                                            " base URL = " + ctx.getRequest().getBaseURL() +
                                            " parameter = " + ctx.getRequest().getParameter() +
                                            " local address = " + ctx.getRequest().getLocalAddress() +
                                            " remote address = " + ctx.getRequest().getRemoteAddress());
                                })
                                .build())
                        .build())
                .build();

        Executor executor = BaseExecutor.builder()
                .build();

        try (SimpleHttpServer server = SimpleHttpsServer.builder()
                .setHttpServerConfig(serverConfig)
                .setApplication(BaseApplication.builder()
                        .setExecutor(executor)
                        .setRouter(router)
                   .build())
                .build()) {
            server.bind();
        }
    }
}
