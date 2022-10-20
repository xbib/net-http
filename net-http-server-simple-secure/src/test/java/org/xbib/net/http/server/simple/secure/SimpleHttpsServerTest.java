package org.xbib.net.http.server.simple.secure;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.BaseApplication;
import org.xbib.net.http.server.BaseHttpDomain;
import org.xbib.net.http.server.route.BaseHttpRouter;
import org.xbib.net.http.server.BaseHttpService;
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
        try (SimpleHttpServer server = SimpleHttpsServer.builder()
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
                                                " SSL peer host = " + ctx.httpRequest().as(HttpsRequest.class).getSSLSession() +
                                                " base URL = " + ctx.httpRequest().getBaseURL() +
                                                " parameter = " + ctx.httpRequest().getParameter() +
                                                " local address = " + ctx.httpRequest().getLocalAddress() +
                                                " remote address = " + ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                        .build())
                   .build())
                .build()) {
            server.bind();
        }
    }
}
