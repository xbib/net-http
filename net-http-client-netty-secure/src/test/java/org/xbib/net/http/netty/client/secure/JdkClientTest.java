package org.xbib.net.http.netty.client.secure;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

/**
 * Testing the JDK 11+ HTTP client for comparison purposes.
 */
public class JdkClientTest {

    private static final Logger logger = Logger.getLogger(JdkClientTest.class.getName());

    static {
        System.setProperty("javax.net.debug", "true");
    }

    @Test
    public void testDNB() throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        Map<String, Object> map = Map.of(
                "version", "1.1",
                "operation", "searchRetrieve",
                "recordSchema", "MARC21plus-1-xml",
                "query", "iss = 00280836"
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://services.dnb.de/sru/zdb"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(buildFormDataFromMap(map))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.log(Level.INFO, Integer.toString(response.statusCode()));
        logger.log(Level.INFO, response.body());
    }

    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<String, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}
