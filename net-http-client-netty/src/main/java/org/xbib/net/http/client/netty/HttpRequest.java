package org.xbib.net.http.client.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.Request;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.BackOff;
import org.xbib.net.http.client.ExceptionListener;
import org.xbib.net.http.client.HttpResponse;
import org.xbib.net.http.client.Part;
import org.xbib.net.http.client.ResponseListener;
import org.xbib.net.http.client.TimeoutListener;
import org.xbib.net.http.cookie.Cookie;

/**
 * HTTP client request.
 */
public class HttpRequest implements org.xbib.net.http.client.HttpRequest, Closeable {

    private final HttpRequestBuilder builder;

    private CompletableFuture<HttpRequest> completableFuture;

    private int redirectCount;

    protected HttpRequest(HttpRequestBuilder builder) {
        this.builder = builder;
    }

    @Override
    public URL getURL() {
        return builder.url;
    }

    @Override
    public HttpVersion getVersion() {
        return builder.httpVersion;
    }

    @Override
    public HttpMethod getMethod() {
        return builder.httpMethod;
    }

    @Override
    public HttpHeaders getHeaders() {
        return builder.headers;
    }

    @Override
    public ParameterBuilder getParameters() {
        return builder.parameterBuilder;
    }

    public Collection<Cookie> cookies() {
        return builder.cookies;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null; // unused
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null; // unused
    }

    @Override
    public URL getBaseURL() {
        return builder.url;
    }

    public ByteBuffer getBody() {
        return builder.body;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return charset.decode(builder.body);
    }

    public CharBuffer getBodyAsChars(Charset charset, int offset, int size) {
        ByteBuffer slicedBuffer = (builder.body.duplicate().position(offset)).slice();
        slicedBuffer.limit(size);
        return charset.decode(slicedBuffer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Request> R as(Class<R> cl) {
        return (R) this;
    }

    public List<Part> getParts() {
        return builder.parts;
    }

    public boolean isFollowRedirect() {
        return builder.followRedirect;
    }

    public boolean isBackOff() {
        return builder.backOff != null;
    }

    public BackOff getBackOff() {
        return builder.backOff;
    }

    public boolean canRedirect() {
        if (!builder.followRedirect) {
            return false;
        }
        if (redirectCount >= builder.maxRedirects) {
            return false;
        }
        redirectCount++;
        return true;
    }

    public void release() {
        // nothing to do
    }

    @Override
    public void close() throws IOException {
        release();
    }

    @Override
    public String toString() {
        return "HttpNettyRequest[url=" + builder.url +
                ",version=" + builder.httpVersion +
                ",method=" + builder.httpMethod +
                ",headers=" + builder.headers.entries() +
                ",content=" + (builder.body != null && builder.body.remaining() >= 16 ?
                getBodyAsChars(StandardCharsets.UTF_8, 0, 16) + "..." :
                builder.body != null ? getBodyAsChars(StandardCharsets.UTF_8) : "") +
                "]";
    }

    public HttpRequest setCompletableFuture(CompletableFuture<HttpRequest> completableFuture) {
        this.completableFuture = completableFuture;
        return this;
    }

    public CompletableFuture<HttpRequest> getCompletableFuture() {
        return completableFuture;
    }

    public void setResponseListener(ResponseListener<HttpResponse> responseListener) {
        builder.responseListener = responseListener;
    }

    public void onResponse(HttpResponse httpResponse) {
        if (builder.responseListener != null) {
            builder.responseListener.onResponse(httpResponse);
        }
        if (completableFuture != null) {
            completableFuture.complete(this);
        }
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        builder.exceptionListener = exceptionListener;
    }

    public void onException(Throwable throwable) {
        if (builder.exceptionListener != null) {
            builder.exceptionListener.onException(throwable);
        }
        if (completableFuture != null) {
            completableFuture.completeExceptionally(throwable);
        }
    }

    public void setTimeoutListener(TimeoutListener timeoutListener) {
        builder.timeoutListener = timeoutListener;
    }

    public void onTimeout() {
        if (builder.timeoutListener != null) {
            builder.timeoutListener.onTimeout(this);
        }
        if (completableFuture != null) {
            if (builder.timeoutMillis > 0L) {
                completableFuture.completeOnTimeout(this, builder.timeoutMillis, TimeUnit.MILLISECONDS);
            } else {
                completableFuture.completeOnTimeout(this, 15L, TimeUnit.SECONDS);
            }
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

    public static HttpRequestBuilder builder(HttpMethod httpMethod, HttpRequest httpRequest) {
        return builder(PooledByteBufAllocator.DEFAULT, httpMethod)
                .setVersion(httpRequest.builder.httpVersion)
                .setURL(httpRequest.builder.url)
                .setHeaders(httpRequest.builder.headers)
                .content(httpRequest.builder.body)
                .setResponseListener(httpRequest.builder.responseListener)
                .setTimeoutListener(httpRequest.builder.timeoutListener, httpRequest.builder.timeoutMillis)
                .setExceptionListener(httpRequest.builder.exceptionListener);
    }

    public static HttpRequestBuilder builder(ByteBufAllocator allocator, HttpMethod httpMethod) {
        return new HttpRequestBuilder(allocator).setMethod(httpMethod);
    }
}
