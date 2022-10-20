package org.xbib.net.http.server.simple.secure;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.simple.HttpRequestBuilder;
import org.xbib.net.http.server.simple.SimpleHttpServer;
import org.xbib.net.http.server.simple.SimpleHttpServerBuilder;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SimpleHttpsServer extends SimpleHttpServer {

    protected SimpleHttpsServer(SimpleHttpServerBuilder builder) {
        super(builder);
    }

    public static SimpleHttpsServerBuilder builder() {
        return new SimpleHttpsServerBuilder();
    }

    @Override
    protected HttpRequestBuilder createRequest(InputStream inputStream,
                                               HttpAddress httpAddress,
                                               InetSocketAddress localAddress,
                                               InetSocketAddress remoteAddress) throws IOException {
        String firstLine = readLine(inputStream);
        HttpVersion httpVersion = extractVersion(firstLine);
        String requestURI = firstLine.split("\\s+", 3)[1];
        HttpMethod httpMethod = extractMethod(firstLine);
        HttpHeaders headers = extractHeaders(inputStream);
        ByteBuffer byteBuffer = extractBody(inputStream, headers);
        return HttpsRequest.builder()
                .setBaseURL(httpAddress,
                        requestURI,
                        headers.get(HttpHeaderNames.HOST))
                .setLocalAddress(localAddress)
                .setRemoteAddress(remoteAddress)
                .setMethod(httpMethod)
                .setVersion(httpVersion)
                .setHeaders(headers)
                .setBody(byteBuffer);
    }

    @Override
    protected ServerSocketFactory getServerSocketFactory(HttpAddress httpAddress) {
        if (httpAddress instanceof HttpsAddress) {
            HttpsAddress httpsAddress = (HttpsAddress) httpAddress;
            return httpsAddress.getSslContext().getServerSocketFactory();
        } else {
            return ServerSocketFactory.getDefault();
        }
    }
}
