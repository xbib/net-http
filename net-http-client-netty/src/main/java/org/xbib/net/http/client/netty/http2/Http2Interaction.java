package org.xbib.net.http.client.netty.http2;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.codec.http2.HttpConversionUtil;
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
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.StreamIds;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.HttpResponse;
import org.xbib.net.http.client.netty.Interaction;
import org.xbib.net.http.client.netty.NettyHttpClient;

public class Http2Interaction extends BaseInteraction {

    private static final Logger logger = Logger.getLogger(Http2Interaction.class.getName());

    public Http2Interaction(NettyHttpClient nettyHttpClient, HttpAddress httpAddress) {
        super(nettyHttpClient, httpAddress);
    }

    @Override
    public Interaction execute(HttpRequest request) throws IOException {
        if (throwable != null) {
            return this;
        }
        Channel channel = acquireChannel(request);
        try {
            waitForSettings(5L, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
        return executeRequest(request, channel);
    }

    public Interaction executeRequest(HttpRequest request, Channel channel) throws IOException {
        this.httpRequest = request;
        final String channelId = channel.id().toString();
        streamIds.putIfAbsent(channelId, new StreamIds());
        ChannelInitializer<Channel> initializer = newHttp2ChildChannelInitializer(nettyHttpClient.getClientConfig(), this, channel);
        Http2StreamChannel childChannel = new Http2StreamChannelBootstrap(channel)
                .handler(initializer).open().syncUninterruptibly().getNow();
        CharSequence method = request.getMethod().name();
        String scheme = request.getURL().getScheme();
        String authority = request.getURL().getHost() + (request.getURL().getPort() != null ? ":" + request.getURL().getPort() : "");
        String relative = request.getURL().relativeReference();
        String path = relative.isEmpty() ? "/" : relative;
        Http2Headers http2Headers = new DefaultHttp2Headers()
                .method(method).scheme(scheme).authority(authority).path(path);
        StreamIds streamIds = super.streamIds.get(channelId);
        if (streamIds == null) {
            throw new IllegalStateException();
        }
        final Integer streamId = streamIds.nextStreamId();
        if (streamId == null) {
            throw new IllegalStateException();
        }
        http2Headers.setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
        // add matching cookies from box (previous requests) and new cookies from request builder
        Collection<Cookie> cookies = new ArrayList<>();
        cookies.addAll(matchCookiesFromBox(request));
        cookies.addAll(matchCookies(request));
        if (!cookies.isEmpty()) {
            request.getHeaders().set(HttpHeaderNames.COOKIE, CookieEncoder.STRICT.encode(cookies));
        }
        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        request.getHeaders().entries().forEach(p -> httpHeaders.set(p.getKey(), p.getValue()));
        HttpConversionUtil.toHttp2Headers(httpHeaders, http2Headers);
        boolean hasContent = request.getBody() != null && request.getBody().remaining() > 0;
        DefaultHttp2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(http2Headers, !hasContent);
        DefaultHttp2DataFrame dataFrame;
        childChannel.write(headersFrame);
        if (hasContent) {
            dataFrame = new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(request.getBody()), true);
            childChannel.write(dataFrame);
        }
        childChannel.flush();
        if (nettyHttpClient.hasPooledNodes()) {
            releaseChannel(channel, false);
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
            logger.log(Level.FINEST, "received settings for promise = " + settingsPromise);
            settingsPromise.setSuccess();
        } else {
            logger.log(Level.WARNING, "settings received but no promise present");
        }
    }

    @Override
    public void responseReceived(Channel channel, Integer streamId, FullHttpResponse fullHttpResponse) {
        if (throwable != null) {
            logger.log(Level.WARNING, "throwable is not null?", throwable);
            return;
        }
        if (streamId == null) {
            logger.log(Level.WARNING, "stream ID is null?");
            return;
        }
        HttpResponse httpResponse = null;
        try {
            // format of childchan channel ID is <parent channel ID> "/" <substream ID>
            String channelId = channel.id().toString();
            int pos = channelId.indexOf('/');
            channelId = pos > 0 ? channelId.substring(0, pos) : channelId;
            StreamIds streamIds = super.streamIds.get(channelId);
            if (streamIds == null) {
                // should never happen
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "stream ID is null? channelId = " + channelId);
                }
                return;
            }
            for (String cookieString : fullHttpResponse.headers().getAll(HttpHeaderNames.SET_COOKIE)) {
                Cookie cookie = CookieDecoder.STRICT.decode(cookieString);
                addCookie(cookie);
            }
            HttpResponseStatus httpStatus = HttpResponseStatus.valueOf(fullHttpResponse.status().code());
            HttpHeaders httpHeaders = new HttpHeaders();
            fullHttpResponse.headers().iteratorCharSequence().forEachRemaining(e -> httpHeaders.add(e.getKey(), e.getValue().toString()));
            httpResponse = newHttpResponseBuilder(channel)
                    .setHttpAddress(httpAddress)
                    .setCookieBox(getCookieBox())
                    .setStatus(httpStatus)
                    .setHeaders(httpHeaders)
                    .setByteBuffer(fullHttpResponse.content().nioBuffer())
                    .build();
            CompletableFuture<Boolean> promise = streamIds.get(streamId);
            try {
                httpRequest.onResponse(httpResponse);
                HttpRequest retryRequest = retry(httpRequest, httpResponse);
                if (retryRequest != null) {
                    // retry transport, wait for completion
                    nettyHttpClient.retry(this, retryRequest);
                } else {
                    HttpRequest continueRequest = continuation(httpRequest, httpResponse);
                    if (continueRequest != null) {
                        // continue with new transport, synchronous call here, wait for completion
                        nettyHttpClient.continuation(this, continueRequest);
                    }
                }
                if (promise != null) {
                    promise.complete(true);
                } else {
                    // when transport is closed, stream IDs will be emptied
                    logger.log(Level.FINE, "promise is null, streamIDs lost");
                }
            } catch (URLSyntaxException | IOException e) {
                if (promise != null) {
                    promise.completeExceptionally(e);
                } else {
                    logger.log(Level.FINE, "promise is null, can't abort");
                }
            } finally {
                streamIds.remove(streamId);
            }
        } finally {
            if (httpResponse != null) {
                httpResponse.release();
            }
        }
    }


    @Override
    public void pushPromiseReceived(Channel channel, Integer streamId, Integer promisedStreamId, Http2Headers headers) {
        String channelId = channel.id().toString();
        StreamIds streamIds = super.streamIds.get(channelId);
        if (streamIds != null) {
            streamIds.put(promisedStreamId, new CompletableFuture<>());
        }
    }

    @Override
    protected String getRequestKey(String channelId, Integer streamId) {
        return channelId + "#" + streamId;
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

    protected Http2ChildChannelInitializer newHttp2ChildChannelInitializer(NettyHttpClientConfig clientConfig,
                                                                           Http2Interaction interaction,
                                                                           Channel parentChannel) {
        return new Http2ChildChannelInitializer(clientConfig, interaction, parentChannel);
    }

    protected HttpResponseBuilder newHttpResponseBuilder(Channel channel) {
        return HttpResponse.builder();
    }
}
