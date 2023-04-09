package org.xbib.net.http.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.Request;
import org.xbib.net.URL;
import org.xbib.net.URLBuilder;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.cookie.Cookie;

public abstract class BaseHttpRequest implements HttpRequest {

    protected final BaseHttpRequestBuilder builder;

    protected BaseHttpRequest(BaseHttpRequestBuilder builder) {
        this.builder = builder;
        Parameter parameter = builder.parameterBuilder.build();
        // validate request
        HttpHeaders validatedHeaders = HttpHeaders.of(builder.httpHeaders);
        if (builder.url != null) {
            // add our URI parameters to the URL
            URLBuilder urlBuilder = builder.url.mutator();
            if (builder.requestPath != null) {
                urlBuilder.path(builder.requestPath);
            }
            parameter.forEach(e -> urlBuilder.queryParam(e.getKey(), e.getValue()));
            builder.url = urlBuilder.build();
            validatedHeaders.set(HttpHeaderNames.HOST, builder.url.getHostInfo());
        }
        validatedHeaders.set(HttpHeaderNames.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        if (builder.userAgent != null) {
            validatedHeaders.set(HttpHeaderNames.USER_AGENT, builder.userAgent);
        }
        if (builder.isGzipEnabled) {
            validatedHeaders.set(HttpHeaderNames.ACCEPT_ENCODING, "gzip");
        }
        if (builder.httpMethod.name().equals(HttpMethod.POST.name())) {
            builder.content(parameter.getAsQueryString(), builder.contentType, StandardCharsets.ISO_8859_1);
        }
        if (!validatedHeaders.containsHeader(HttpHeaderNames.CONTENT_LENGTH) &&
                !validatedHeaders.containsHeader(HttpHeaderNames.TRANSFER_ENCODING)) {
            int length = builder.byteBuffer != null ? builder.byteBuffer.remaining() : 0;
            if (length < 0) {
                validatedHeaders.set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
            } else {
                validatedHeaders.set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(length));
            }
        }
        if (!validatedHeaders.containsHeader(HttpHeaderNames.ACCEPT)) {
            validatedHeaders.set(HttpHeaderNames.ACCEPT, "*/*");
        }
        // RFC 2616 Section 14.10
        // "An HTTP/1.1 client that does not support persistent connections MUST include the "close" connection
        // option in every request message."
        if (builder.httpVersion.majorVersion() == 1 && !builder.isKeepAliveEnabled) {
            validatedHeaders.set(HttpHeaderNames.CONNECTION, "close");
        }
        builder.setHeaders(validatedHeaders);
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return builder.localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return builder.remoteAddress;
    }

    @Override
    public URL getBaseURL() {
        return URL.builder()
                .scheme(builder.url.getScheme())
                .host(builder.url.getHost())
                .port(builder.url.getPort())
                .build();
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
        return builder.httpHeaders;
    }

    @Override
    public ParameterBuilder getParameterBuilder() {
        return builder.parameterBuilder;
    }

    @Override
    public ByteBuffer getBody() {
        return builder.byteBuffer;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return builder.byteBuffer != null ? charset.decode(builder.byteBuffer) : null;
    }

    public CharBuffer getBodyAsChars(Charset charset, int offset, int size) {
        ByteBuffer slicedBuffer = (builder.byteBuffer.position(offset)).slice();
        slicedBuffer.limit(size);
        return charset.decode(slicedBuffer);
    }

    @Override
    public List<Message> getMessages() {
        return builder.messages;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Request> R as(Class<R> cl) {
        return (R) this;
    }

    public boolean canRedirect() {
        if (!builder.followRedirect) {
            return false;
        }
        if (builder.redirectCount >= builder.maxRedirects) {
            return false;
        }
        builder.redirectCount++;
        return true;
    }

    @Override
    public boolean isBackOffEnabled() {
        return builder.isBackoffEnabled;
    }

    @Override
    public BackOff getBackOff() {
        return builder.backOff;
    }

    @Override
    public Collection<Cookie> cookies() {
        return builder.cookies;
    }

    public HttpRequest setCompletableFuture(CompletableFuture<HttpRequest> completableFuture) {
        builder.completableFuture = completableFuture;
        return this;
    }

    public CompletableFuture<HttpRequest> getCompletableFuture() {
        return builder.completableFuture;
    }

    public void setResponseListener(ResponseListener<HttpResponse> responseListener) {
        builder.responseListener = responseListener;
    }

    public ResponseListener<HttpResponse> getResponseListener() {
        return builder.responseListener;
    }

    public void onResponse(HttpResponse httpResponse) {
        if (builder.responseListener != null) {
            builder.responseListener.onResponse(httpResponse);
        }
        if (builder.completableFuture != null) {
            builder.completableFuture.complete(this);
        }
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        builder.exceptionListener = exceptionListener;
    }

    public ExceptionListener getExceptionListener() {
        return builder.exceptionListener;
    }

    public void onException(Throwable throwable) {
        if (builder.exceptionListener != null) {
            builder.exceptionListener.onException(throwable);
        }
        if (builder.completableFuture != null) {
            builder.completableFuture.completeExceptionally(throwable);
        }
    }

    public void setTimeoutListener(TimeoutListener timeoutListener) {
        builder.timeoutListener = timeoutListener;
    }

    public TimeoutListener getTimeoutListener() {
        return builder.timeoutListener;
    }

    public void onTimeout() {
        if (builder.timeoutListener != null) {
            builder.timeoutListener.onTimeout(this);
        }
        if (builder.completableFuture != null) {
            if (builder.timeoutMillis > 0L) {
                builder.completableFuture.completeOnTimeout(this, builder.timeoutMillis, TimeUnit.MILLISECONDS);
            } else {
                builder.completableFuture.completeOnTimeout(this, 15L, TimeUnit.SECONDS);
            }
        }
    }

    public long getTimeoutMillis() {
        return builder.timeoutMillis;
    }

    @Override
    public String toString() {
        return "HttpRequest[url=" + builder.url +
                ",version=" + builder.httpVersion +
                ",method=" + builder.httpMethod +
                ",headers=" + builder.httpHeaders.entries() +
                ",content=" + (builder.byteBuffer != null && builder.byteBuffer.remaining() >= 16 ?
                getBodyAsChars(StandardCharsets.UTF_8, 0, 16) + "..." :
                builder.byteBuffer != null ? getBodyAsChars(StandardCharsets.UTF_8) : "") +
                ",messages=" + builder.messages +
                "]";
    }
}
