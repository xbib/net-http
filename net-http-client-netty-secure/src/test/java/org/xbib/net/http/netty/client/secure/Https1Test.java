package org.xbib.net.http.netty.client.secure;

import io.netty.handler.proxy.Socks5ProxyHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.SocketConfig;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.client.HttpResponse;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.HttpsResponse;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Https1Test {

    private static final Logger logger = Logger.getLogger(Https1Test.class.getName());

    @Test
    void testXbib() throws Exception {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://xbib.org")
                    .setResponseListener(resp ->
                            logger.log(Level.INFO,
                                    "got response: " +
                                            " status = " + resp.getStatus() +
                                            " headers = " + resp.getHeaders() +
                                            " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                            " ssl = " + dumpCertificates((HttpsResponse) resp)))
                        .build();
                client.execute(request).get().close();
        }
    }

    @Test
    void testGoogleHttp() throws Exception {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setProtocolNegotiation(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("http://google.de")
                    .setResponseListener(resp ->
                            logger.log(Level.INFO,
                                    "got response: " +
                                            " status = " + resp.getStatus() +
                                            " headers = " + resp.getHeaders() +
                                            " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8)))
                    .build();
            client.execute(request).get().close();
        }
    }

    @Test
    void testGoogleUpgradeHttps() throws Exception {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setProtocolNegotiation(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://www.google.de/")
                    .setResponseListener(resp ->
                            logger.log(Level.INFO,
                                    "got response: " +
                                            " status = " + resp.getStatus() +
                                            " headers = " + resp.getHeaders() +
                                            " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                            " ssl = " + dumpCertificates((HttpsResponse) resp)))
                    .build();
            client.execute(request).get().close();
        }
    }

    @Test
    void testDNB() throws Exception {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            Map<String, Object> map = Map.of(
                    "version", "1.1",
                    "operation", "searchRetrieve",
                    "recordSchema", "MARC21plus-1-xml",
                    "query", "iss=00280836"
            );
            HttpRequest request = HttpRequest.get()
                    .setURL("http://services.dnb.de/sru/zdb")
                    .setParameters(map)
                    .setResponseListener(resp -> logger.log(Level.INFO,
                            "got response: " + resp.getHeaders() +
                                    resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                    " status=" + resp.getStatus()))
                    .build();
            client.execute(request).get().close();
        }
    }

    @Test
    void testHebisGetRequest() throws Exception {
        // we test HEBIS here with strange certificate setup and TLS 1.2 only
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()){
            HttpRequest request = HttpRequest.post()
                    .setURL("https://hebis.rz.uni-frankfurt.de/HEBCGI/vuefl_recv_data.pl")
                    .setResponseListener(resp ->
                            logger.log(Level.INFO,
                                    "got response: " +
                                            " status = " + resp.getStatus() +
                                            " headers = " + resp.getHeaders() +
                                            " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                            " ssl = " + dumpCertificates((HttpsResponse) resp))
                    )
                    .build();
            client.execute(request).get().close();
        }
    }

    @Test
    void testSequentialRequests() throws Exception {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            for (int i = 0; i <10; i++) {
                HttpRequest request = HttpRequest.get().setURL("https://xbib.org")
                        .setResponseListener(resp ->
                                logger.log(Level.INFO,
                                        "got response: " +
                                                " status = " + resp.getStatus() +
                                                " headers = " + resp.getHeaders() +
                                                " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                                " ssl = " + dumpCertificates((HttpsResponse) resp)))
                        .build();
                client.execute(request).get();
            }
        }
    }

    @Test
    void testParallelRequests() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        NettyHttpClientConfig config = new NettyHttpsClientConfig();
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request1 = HttpRequest.builder(HttpMethod.GET)
                    .setURL("https://xbib.org")
                    .setVersion("HTTP/1.1")
                    .setResponseListener(resp ->
                            logger.log(Level.INFO,
                                    "got response: " +
                                            " counter = " + counter.incrementAndGet() +
                                            " status = " + resp.getStatus() +
                                            " headers = " + resp.getHeaders() +
                                            " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                            " ssl = " + dumpCertificates((HttpsResponse) resp)))
                    .build();
            HttpRequest request2 = HttpRequest.builder(HttpMethod.GET)
                    .setURL("https://xbib.org")
                    .setVersion("HTTP/1.1")
                    .setResponseListener(resp ->
                            logger.log(Level.INFO,
                                    "got response: " +
                                            " counter = " + counter.incrementAndGet() +
                                            " status = " + resp.getStatus() +
                                            " headers = " + resp.getHeaders() +
                                            " body = " + resp.getBodyAsChars(StandardCharsets.UTF_8) +
                                            " ssl = " + dumpCertificates((HttpsResponse) resp)))
                    .build();
            for (int i = 0; i < 5; i++) {
                client.execute(request1);
                client.execute(request2);
            }
            Thread.sleep(1000L);
        }
        assertEquals(10, counter.get());
    }

    @Test
    void testXbibOrgWithCompletableFuture() throws IOException {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient httpClient = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            HttpRequest request = HttpRequest.get()
                    .setURL("https://xbib.org")
                    .build();
            String result = httpClient.execute(request, response -> response.getBodyAsChars(StandardCharsets.UTF_8).toString())
                    .exceptionally(Throwable::getMessage)
                    .join();
            logger.info("got result = " + result);
        }
        // TODO 15 sec timeout on closing event loop group, why?
    }

    @Test
    void testXbibOrgWithCompletableFutureAndGoogleSearch() throws IOException {
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setDebug(true);
        try (NettyHttpClient httpClient = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            final Function<HttpResponse, String> stringFunction =
                    response -> response.getBodyAsChars(StandardCharsets.UTF_8).toString();
            HttpRequest request = HttpRequest.get()
                    .setURL("https://xbib.org")
                    .build();
            final CompletableFuture<String> completableFuture = httpClient.execute(request, stringFunction)
                    .exceptionally(Throwable::getMessage)
                    .thenCompose(content -> {
                        try {
                            return httpClient.execute(HttpRequest.get()
                                    .setURL("https://www.google.de/")
                                    .addParameter("query", content.substring(0, 15))
                                    .build(), stringFunction);
                        } catch (IOException e) {
                            logger.log(Level.WARNING, e.getMessage(), e);
                            return null;
                        }
                    });
            String result = completableFuture.join();
            logger.info("got result = " + result);
        }
    }

    @Disabled("proxy is down")
    @Test
    void testXbibOrgWithProxy() throws IOException {
        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setConnectTimeoutMillis(30000);
        socketConfig.setReadTimeoutMillis(30000);
        Socks5ProxyHandler handler = new Socks5ProxyHandler(new InetSocketAddress("178.162.202.44", 1695));
        handler.setConnectTimeoutMillis(30000L);
        NettyHttpClientConfig config = new NettyHttpsClientConfig()
                .setSocketConfig(socketConfig)
                .setSocks5ProxyHandler(handler)
                .setDebug(true);
        try (NettyHttpClient httpClient = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
            httpClient.execute(HttpRequest.get()
                            .setURL("https://xbib.org")
                            .setResponseListener(resp -> logger.log(Level.INFO, "status = " + resp.getStatus() +
                                    " response body = " + resp.getBodyAsChars(StandardCharsets.UTF_8)))
                            .build())
                    .get();
        }
    }

    private String dumpCertificates(HttpsResponse httpsResponse) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Certificate certificate : httpsResponse.getSSLSession().getPeerCertificates()) {
                if (certificate instanceof X509Certificate) {
                    X509Certificate c = (X509Certificate) certificate;
                    sb.append("subjects=").append(c.getSubjectAlternativeNames());
                    sb.append(",issuers=").append(c.getIssuerAlternativeNames());
                    sb.append(",not before=").append(c.getNotBefore());
                    sb.append(",not after=").append(c.getNotAfter());
                    sb.append("\n");
                }
            }
        } catch (SSLPeerUnverifiedException | CertificateParsingException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return sb.toString();
    }
}
