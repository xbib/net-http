package org.xbib.net.http.client.netty.http1;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.URLSyntaxException;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.client.cookie.CookieDecoder;
import org.xbib.net.http.client.cookie.CookieEncoder;
import org.xbib.net.http.client.netty.BaseInteraction;
import org.xbib.net.http.client.netty.HttpResponseBuilder;
import org.xbib.net.http.client.netty.StreamIds;
import org.xbib.net.http.client.netty.http2.Http2Interaction;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.HttpResponse;
import org.xbib.net.http.client.netty.Interaction;
import org.xbib.net.http.client.netty.NettyHttpClient;

public class Http1Interaction extends BaseInteraction {

    private static final Logger logger = Logger.getLogger(Http1Interaction.class.getName());

    private final HttpDataFactory httpDataFactory;

    public Http1Interaction(NettyHttpClient nettyHttpClient, HttpAddress httpAddress) {
        super(nettyHttpClient, httpAddress);
        this.httpDataFactory = new DefaultHttpDataFactory();
    }

    @Override
    public Interaction execute(HttpRequest request) throws IOException {
        if (throwable != null) {
            logger.log(Level.WARNING, throwable.getMessage(), throwable);
            return this;
        }
        httpRequest = request;
        Channel channel = acquireChannel(request);
        try {
            // if http2Settings is present, we have a HTTP-2 upgrade
            waitForSettings(5L, TimeUnit.SECONDS);
            if (http2Settings != null) {
                Http2Interaction interaction = upgradeInteraction();
                interaction.executeRequest(request, channel);
                return interaction;
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
        return executeRequest(request, channel);
    }

    public Interaction executeRequest(HttpRequest request, Channel channel) throws IOException {
        final String channelId = channel.id().toString();
        streamIds.putIfAbsent(channelId, new StreamIds());
        // Some HTTP 1 servers do not understand URIs in HTTP command line in spite of RFC 7230.
        // The "origin form" requires a "Host" header.
        // Our algorithm is: use always "origin form" for HTTP 1, use absolute form for HTTP 2.
        // The reason is that Netty derives the HTTP/2 scheme header from the absolute form.
        String uri = request.getVersion().majorVersion() == 1 ? request.getURL().relativeReference() : request.getURL().toExternalForm();
        HttpVersion httpVersion = HttpVersion.valueOf(request.getVersion().text());
        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().name());
        DefaultFullHttpRequest fullHttpRequest = request.getBody() == null ?
                new DefaultFullHttpRequest(httpVersion, httpMethod, uri) :
                new DefaultFullHttpRequest(httpVersion, httpMethod, uri, Unpooled.wrappedBuffer(request.getBody()));
        HttpPostRequestEncoder httpPostRequestEncoder = null;
        final Integer streamId = streamIds.get(channelId).nextStreamId();
        if (streamId == null) {
            throw new IllegalStateException("stream id is null");
        }
        // add matching cookies from box (previous requests) and new cookies from request builder
        Collection<Cookie> cookies = new ArrayList<>();
        cookies.addAll(matchCookiesFromBox(request));
        cookies.addAll(matchCookies(request));
        if (!cookies.isEmpty()) {
            request.getHeaders().set(HttpHeaderNames.COOKIE, CookieEncoder.STRICT.encode(cookies));
        }
        request.getHeaders().entries().forEach(p -> fullHttpRequest.headers().add(p.getKey(), p.getValue()));
        if (request.getBody() == null && !request.getBodyData().isEmpty()) {
            try {
                httpPostRequestEncoder = new HttpPostRequestEncoder(httpDataFactory, fullHttpRequest, true);
                httpPostRequestEncoder.setBodyHttpDatas(request.getBodyData());
                httpPostRequestEncoder.finalizeRequest();
            } catch (HttpPostRequestEncoder.ErrorDataEncoderException e) {
                throw new IOException(e);
            }
        }
        if (!channel.isWritable()) {
            logger.log(Level.WARNING, "channel not writable");
            return this;
        }
        channel.write(fullHttpRequest);
        if (httpPostRequestEncoder != null && httpPostRequestEncoder.isChunked()) {
            channel.write(httpPostRequestEncoder);
        }
        channel.flush();
        if (httpPostRequestEncoder != null) {
            httpPostRequestEncoder.cleanFiles();
        }
        return this;
    }

    @Override
    public void settingsPrefaceWritten() {
        logger.log(Level.FINEST, "settings/preface written");
    }

    @Override
    public void waitForSettings(long value, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        if (settingsPromise != null) {
            logger.log(Level.FINEST, "waiting for settings, promise = " + settingsPromise);
            settingsPromise.get(value, timeUnit);
        }
    }

    @Override
    public void settingsReceived(Http2Settings http2Settings) {
        this.http2Settings = http2Settings;
        if (settingsPromise != null) {
            logger.log(Level.FINEST, "received settings " + http2Settings + " for promise " + settingsPromise);
            if (!settingsPromise.isDone()) {
                settingsPromise.setSuccess();
            }
        } else {
            logger.log(Level.WARNING, "settings received but no promise present");
        }
    }

    @Override
    public void responseReceived(Channel channel, Integer streamId, FullHttpResponse fullHttpResponse) {
        if (throwable != null) {
            logger.log(Level.WARNING, "throwable not null", throwable);
            return;
        }
        HttpResponse httpResponse = null;
        try {
            // streamID is expected to be null, last request on memory
            // is expected to be current, remove request from memory
            for (String cookieString : fullHttpResponse.headers().getAll(HttpHeaderNames.SET_COOKIE)) {
                Cookie cookie = CookieDecoder.STRICT.decode(cookieString);
                addCookie(cookie);
            }
            HttpResponseStatus httpStatus = HttpResponseStatus.valueOf(fullHttpResponse.status().code());
            HttpHeaders httpHeaders = new HttpHeaders();
            fullHttpResponse.headers().iteratorCharSequence().forEachRemaining(e -> httpHeaders.add(e.getKey(), e.getValue().toString()));
            httpResponse = newHttpResponseBuilder(channel)
                    .setHttpAddress(httpAddress)
                    .setLocalAddress(channel.localAddress())
                    .setRemoteAddress(channel.remoteAddress())
                    .setCookieBox(getCookieBox())
                    .setStatus(httpStatus)
                    .setHeaders(httpHeaders)
                    .setByteBuffer(fullHttpResponse.content().nioBuffer())
                    .build();
            httpRequest.onResponse(httpResponse);
            // check for retry / continue
            try {
                HttpRequest retryRequest = retry(httpRequest, httpResponse);
                if (retryRequest != null) {
                    // retry transport, wait for completion
                    nettyHttpClient.retry(this, retryRequest);
                } else {
                    HttpRequest continueRequest = continuation(httpRequest, httpResponse);
                    if (continueRequest != null) {
                        // continue with new transport, synchronous call here,
                        // wait for completion
                        nettyHttpClient.continuation(this, continueRequest);
                    }
                }
            } catch (URLSyntaxException | IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
            // acknowledge success, if possible
            String channelId = channel.id().toString();
            StreamIds streamIds = super.streamIds.get(channelId);
            if (streamIds != null) {
                Integer lastKey = streamIds.lastKey();
                if (lastKey != null) {
                    CompletableFuture<Boolean> promise = streamIds.get(lastKey);
                    if (promise != null) {
                        promise.complete(true);
                    }
                }
            }
        } finally {
            if (httpResponse != null) {
                httpResponse.release();
            }
        }
    }

    @Override
    public void pushPromiseReceived(Channel channel, Integer streamId,
                                    Integer promisedStreamId, Http2Headers headers) {
    }

    @Override
    protected String getRequestKey(String channelId, Integer streamId) {
        return null;
    }

    @Override
    protected Channel nextChannel() throws IOException {
        Channel channel = newChannel(httpAddress);
        if (channel == null) {
            ConnectException connectException;
            if (httpAddress != null) {
                connectException = new ConnectException("unable to connect to " + httpAddress);
            } else if (nettyHttpClient.hasPooledNodes()) {
                connectException = new ConnectException("unable to get channel from pool");
            } else {
                // API misuse
                connectException = new ConnectException("unable to get channel");
            }
            this.throwable = connectException;
            throw connectException;
        }
        return channel;
    }

    @Override
    public void close() throws IOException {
        httpDataFactory.cleanAllHttpData();
        super.close();
    }

    protected HttpResponseBuilder newHttpResponseBuilder(Channel channel) {
        return HttpResponse.builder();
    }

    protected Http2Interaction upgradeInteraction() {
        return new Http2Interaction(nettyHttpClient, httpAddress);
    }
}
