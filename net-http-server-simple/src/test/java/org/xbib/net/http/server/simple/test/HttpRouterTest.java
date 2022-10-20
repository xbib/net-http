package org.xbib.net.http.server.simple.test;

import org.junit.jupiter.api.Test;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.BaseApplication;
import org.xbib.net.http.server.BaseHttpDomain;
import org.xbib.net.http.server.route.BaseHttpRouter;
import org.xbib.net.http.server.BaseHttpService;
import org.xbib.net.http.server.simple.HttpRequest;
import org.xbib.net.http.server.simple.HttpRequestBuilder;
import org.xbib.net.http.server.simple.HttpResponse;
import org.xbib.net.http.server.simple.HttpResponseBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpRouterTest {

    @Test
    public void routerTest() throws Exception {
        URL baseURL = URL.http().host("localhost").port(8008).build();
        HttpAddress httpAddress = HttpAddress.of(baseURL);
        BaseHttpRouter router = BaseHttpRouter.builder()
                .addDomain(BaseHttpDomain.builder()
                        .setHttpAddress(httpAddress)
                        .addService(BaseHttpService.builder()
                                .setMethod(HttpMethod.DELETE)
                                .setPath("/demo")
                                .setHandler(ctx -> {
                                    Logger.getAnonymousLogger().log(Level.INFO, "got request: " + ctx.request().getRequestURI());
                                    ctx.response()
                                            .setResponseStatus(HttpResponseStatus.OK)
                                            .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                            .setCharset(StandardCharsets.UTF_8);
                                    ctx.write(ctx.request().getRequestURI());
                                })
                                .build())
                        .build())
                .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpResponseBuilder httpResponse = HttpResponse.builder()
                .setOutputStream(outputStream);
        HttpRequestBuilder httpRequest = HttpRequest.builder()
                .setBaseURL(baseURL)
                .setVersion(HttpVersion.HTTP_1_1)
                .setMethod(HttpMethod.DELETE)
                .setRequestURI("/demo")
                .addHeader(HttpHeaderNames.HOST, httpAddress.hostAndPort());
        router.setApplication(BaseApplication.builder().build());
        router.route(httpRequest, httpResponse);
        String string = outputStream.toString(StandardCharsets.UTF_8);
        Logger.getAnonymousLogger().log(Level.INFO, "the response string is = " + string);
        assertTrue(string.contains("/demo"));
    }
}
