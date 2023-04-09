package org.xbib.net.http.client.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2Settings;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.xbib.net.PercentDecoder;
import org.xbib.net.URL;
import org.xbib.net.URLSyntaxException;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.client.BackOff;
import org.xbib.net.http.client.HttpResponse;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.cookie.CookieBox;

public abstract class BaseInteraction implements Interaction {

    private static final Logger logger = Logger.getLogger(BaseInteraction.class.getName());

    protected final NettyHttpClient nettyHttpClient;

    protected final HttpAddress httpAddress;

    protected Throwable throwable;

    protected final Map<String, StreamIds> streamIds;

    protected HttpRequest httpRequest;

    protected Channel channel;

    private CookieBox cookieBox;

    protected ChannelPromise settingsPromise;

    protected Http2Settings http2Settings;

    protected CompletableFuture<?> future;

    public BaseInteraction(NettyHttpClient nettyHttpClient, HttpAddress httpAddress) {
        this.nettyHttpClient = nettyHttpClient;
        this.httpAddress = httpAddress;
        this.streamIds = new ConcurrentHashMap<>();
    }

    @Override
    public void setSettingsPromise(ChannelPromise settingsPromise) {
        this.settingsPromise = settingsPromise;
    }

    @Override
    public HttpAddress getHttpAddress() {
        return httpAddress;
    }

    public void setFuture(CompletableFuture<?> future) {
        this.future = future;
    }

    public CompletableFuture<?> getFuture() {
        return future;
    }

    /**
     * Method for executing the request and respond in a completable future.
     *
     * @param request request
     * @param supplier supplier
     * @param <T> supplier result
     * @return completable future
     */
    @Override
    public <T> CompletableFuture<T> execute(HttpRequest request, Function<HttpResponse, T> supplier)
            throws IOException {
        Objects.requireNonNull(request);
        this.httpRequest = request;
        Objects.requireNonNull(supplier);
        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        request.setResponseListener(response -> {
            if (response != null) {
                completableFuture.complete(supplier.apply(response));
            } else {
                completableFuture.cancel(true);
            }
            get();
            cancel();
        });
        request.setTimeoutListener(req -> completableFuture.completeExceptionally(new TimeoutException()));
        request.setExceptionListener(completableFuture::completeExceptionally);
        execute(request);
        return completableFuture;
    }

    @Override
    public void close() throws IOException {
        logger.log(Level.FINE, "closing interaction " + this);
        get();
        //cancel();
        releaseChannel(channel, true);
        if (future != null) {
            future.complete(null);
        }
    }

    @Override
    public boolean isFailed() {
        return throwable != null;
    }

    @Override
    public Throwable getFailure() {
        return throwable;
    }

    /**
     * The underlying network layer failed.
     * So we fail all (open) promises.
     * @param throwable the exception
     */
    @Override
    public void fail(Channel channel, Throwable throwable) {
        // do not fail more than once
        if (this.throwable != null) {
            return;
        }
        this.throwable = throwable;
        logger.log(Level.SEVERE, "channel " + channel + " failing: " + throwable.getMessage(), throwable);
        for (StreamIds streamIds : streamIds.values()) {
            streamIds.fail(throwable);
        }
        if (future != null) {
            future.completeExceptionally(throwable);
        }
    }

    @Override
    public void inactive(Channel channel) {
        // do nothing
    }

    @Override
    public Interaction get() {
        return get(nettyHttpClient.getClientConfig().getSocketConfig().getReadTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Interaction get(long value, TimeUnit timeUnit) {
        if (!streamIds.isEmpty()) {
            for (Map.Entry<String, StreamIds> entry : streamIds.entrySet()) {
                StreamIds streamIds = entry.getValue();
                if (!streamIds.isClosed()) {
                    for (Integer key : streamIds.keys()) {
                        String requestKey = getRequestKey(entry.getKey(), key);
                        try {
                            CompletableFuture<Boolean> timeoutFuture = streamIds.get(key);
                            Boolean timeout = timeoutFuture.get(value, timeUnit);
                            if (timeout) {
                                completeRequest(requestKey);
                            } else {
                                completeRequestTimeout(requestKey, new TimeoutException());
                            }
                        } catch (TimeoutException e) {
                            completeRequestTimeout(requestKey, new TimeoutException());
                        } catch (Exception e) {
                            completeRequestExceptionally(requestKey, e);
                            streamIds.fail(e);
                        } finally {
                            streamIds.remove(key);
                        }
                    }
                     streamIds.close();
                }
            }
        }
        nettyHttpClient.remove(this);
        return this;
    }

    @Override
    public void cancel() {
        if (!streamIds.isEmpty()) {
            for (Map.Entry<String, StreamIds> entry : streamIds.entrySet()) {
                StreamIds streamIds = entry.getValue();
                for (Integer key : streamIds.keys()) {
                    try {
                        streamIds.get(key).cancel(true);
                    } catch (Exception e) {
                        completeRequestExceptionally(getRequestKey(entry.getKey(), key), e);
                        streamIds.fail(e);
                    } finally {
                        streamIds.remove(key);
                    }
                }
                streamIds.close();
            }
            streamIds.clear();
        }
    }

    protected abstract String getRequestKey(String channelId, Integer streamId);

    protected Channel acquireChannel(HttpRequest request) throws IOException {
        Channel channel;
        if (nettyHttpClient.hasPooledNodes()) {
            channel = nextChannel();
            this.channel = channel;
        } else {
            channel = this.channel;
            if (channel == null) {
                channel = nextChannel();
            }
            this.channel = channel;
        }
        return channel;
    }

    protected Channel newChannel(HttpAddress httpAddress) throws IOException {
        if (httpAddress != null) {
            try {
                return nettyHttpClient.getBootstrap()
                        .handler(nettyHttpClient.newChannelInitializer(httpAddress, this))
                        .connect(httpAddress.getInetSocketAddress()).sync().await().channel();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        } else {
            if (nettyHttpClient.hasPooledNodes()) {
                try {
                    return nettyHttpClient.getPool().acquire();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    protected void releaseChannel(Channel channel, boolean close) throws IOException{
        if (channel == null) {
            return;
        }
        if (nettyHttpClient.hasPooledNodes()) {
            try {
                nettyHttpClient.getPool().release(channel, close);
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else if (close) {
            channel.close();
        }
    }

    protected abstract Channel nextChannel() throws IOException;

    protected HttpRequest continuation(HttpRequest request,
                                       HttpResponse httpResponse) throws URLSyntaxException {
        if (httpResponse == null) {
            return null;
        }
        if (request == null) {
            // push promise or something else
            return null;
        }
        try {
            if (request.canRedirect()) {
                int status = httpResponse.getStatus().code();
                switch (status) {
                    case 300, 301, 302, 303, 305, 307, 308 -> {
                        String location = httpResponse.getHeaders().get(HttpHeaderNames.LOCATION);
                        location = new PercentDecoder(StandardCharsets.UTF_8.newDecoder()).decode(location);
                        if (location != null) {
                            logger.log(Level.FINE, "found redirect location: " + location);
                            URL redirUrl = URL.base(request.getURL()).resolve(location);
                            HttpMethod method = httpResponse.getStatus().code() == 303 ? HttpMethod.GET : request.getMethod();
                            HttpRequestBuilder newHttpRequestHttpRequestBuilder = HttpRequest.builder(method, request)
                                    .setURL(redirUrl);
                            request.getURL().getQueryParams().forEach(pair ->
                                    newHttpRequestHttpRequestBuilder.addParameter(pair.getKey(), pair.getValue())
                            );
                            request.cookies().forEach(newHttpRequestHttpRequestBuilder::addCookie);
                            HttpRequest newHttpRequest = newHttpRequestHttpRequestBuilder.build();
                            StringBuilder hostAndPort = new StringBuilder();
                            hostAndPort.append(redirUrl.getHost());
                            if (redirUrl.getPort() != null) {
                                hostAndPort.append(':').append(redirUrl.getPort());
                            }
                            newHttpRequest.getHeaders().set(HttpHeaderNames.HOST, hostAndPort.toString());
                            logger.log(Level.FINE, "redirect url: " + redirUrl);
                            return newHttpRequest;
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (MalformedInputException | UnmappableCharacterException e) {
            this.throwable = e;
        }
        return null;
    }

    protected HttpRequest retry(HttpRequest request, HttpResponse httpResponse) {
        if (httpResponse == null) {
            // no response present, invalid in any way
            return null;
        }
        if (request == null) {
            // push promise or something else
            return null;
        }
        if (request.isBackOffEnabled()) {
            BackOff backOff = request.getBackOff() != null ?
                    request.getBackOff() :
                    nettyHttpClient.getClientConfig().getBackOff();
            int status = httpResponse.getStatus ().code();
            switch (status) {
                case 403, 404, 500, 502, 503, 504, 507, 509 -> {
                    if (backOff != null) {
                        long millis = backOff.nextBackOffMillis();
                        if (millis != BackOff.STOP) {
                            logger.log(Level.FINE, () -> "status = " + status + " backing off request by " + millis + " milliseconds");
                            try {
                                Thread.sleep(millis);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            return request;
                        }
                    }
                }
                default -> {
                }
            }
        }
        return null;
    }

    private void completeRequest(String requestKey) {
        if (requestKey != null) {
            if (httpRequest != null && httpRequest.getCompletableFuture() != null) {
                httpRequest.getCompletableFuture().complete(httpRequest);
            }
        }
    }

    private void completeRequestExceptionally(String requestKey, Throwable throwable) {
        if (requestKey != null) {
            httpRequest.onException(throwable);
        }
    }

    private void completeRequestTimeout(String requestKey, TimeoutException timeoutException) {
        if (requestKey != null) {
            httpRequest.onTimeout();
        }
    }

    @Override
    public void setCookieBox(CookieBox cookieBox) {
        this.cookieBox = cookieBox;
    }

    @Override
    public CookieBox getCookieBox() {
        return cookieBox;
    }

    protected void addCookie(Cookie cookie) {
        if (cookieBox == null) {
            this.cookieBox = new CookieBox();
        }
        cookieBox.add(cookie);
    }

    protected List<Cookie> matchCookiesFromBox(HttpRequest request) {
        return cookieBox == null ? Collections.emptyList() : cookieBox.stream().filter(cookie ->
                matchCookie(request.getURL(), cookie)).collect(Collectors.toList());
    }

    protected List<Cookie> matchCookies(HttpRequest request) {
        return request.cookies().stream().filter(cookie ->
                matchCookie(request.getURL(), cookie)).collect(Collectors.toList());
    }

    private boolean matchCookie(URL url, Cookie cookie) {
        boolean domainMatch = cookie.domain() == null || url.getHost().endsWith(cookie.domain());
        if (!domainMatch) {
            return false;
        }
        if (cookie.path() != null) {
            boolean pathMatch = "/".equals(cookie.path()) || url.getPath().startsWith(cookie.path());
            if (!pathMatch) {
                return false;
            }
        }
        boolean secureScheme = "https".equals(url.getScheme());
        return (secureScheme && cookie.isSecure()) || (!secureScheme && !cookie.isSecure());
    }
}
