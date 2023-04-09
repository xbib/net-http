package org.xbib.net.http.client.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http2.HttpConversionUtil;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.client.BaseHttpRequest;

/**
 * Netty HTTP client request.
 */
public class HttpRequest extends BaseHttpRequest {

    protected HttpRequest(HttpRequestBuilder builder) {
        super(builder);
        String scheme = builder.getUrl().getScheme();
        if (this.builder.getVersion().majorVersion() == 2) {
            this.builder.addHeader(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text().toString(), scheme);
        }
    }

    public static HttpRequestBuilder get() {
        return builder(HttpMethod.GET);
    }

    public static HttpRequestBuilder put() {
        return builder(HttpMethod.PUT);
    }

    public static HttpRequestBuilder post() {
        return builder(HttpMethod.POST);
    }

    public static HttpRequestBuilder delete() {
        return builder(HttpMethod.DELETE);
    }

    public static HttpRequestBuilder head() {
        return builder(HttpMethod.HEAD);
    }

    public static HttpRequestBuilder patch() {
        return builder(HttpMethod.PATCH);
    }

    public static HttpRequestBuilder trace() {
        return builder(HttpMethod.TRACE);
    }

    public static HttpRequestBuilder options() {
        return builder(HttpMethod.OPTIONS);
    }

    public static HttpRequestBuilder connect() {
        return builder(HttpMethod.CONNECT);
    }

    public static HttpRequestBuilder builder(HttpMethod httpMethod) {
        return builder(PooledByteBufAllocator.DEFAULT, httpMethod);
    }

    public static HttpRequestBuilder builder(ByteBufAllocator allocator, HttpMethod httpMethod) {
        return new HttpRequestBuilder(allocator)
                .setMethod(httpMethod);
    }

    public static HttpRequestBuilder builder(HttpMethod httpMethod, HttpRequest httpRequest) {
        return builder(PooledByteBufAllocator.DEFAULT, httpMethod)
                .setVersion(httpRequest.getVersion())
                .setURL(httpRequest.getURL())
                .setHeaders(httpRequest.getHeaders())
                .content(httpRequest.getBody())
                .setResponseListener(httpRequest.getResponseListener())
                .setTimeoutListener(httpRequest.getTimeoutListener(), httpRequest.getTimeoutMillis())
                .setExceptionListener(httpRequest.getExceptionListener());
    }
}
