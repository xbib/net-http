package org.xbib.net.http.client.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.client.HttpResponse;
import org.xbib.net.http.cookie.CookieBox;

public interface Interaction extends Closeable {

    HttpAddress getHttpAddress();

    Interaction execute(HttpRequest httpRequest) throws IOException;

    <T> CompletableFuture<T> execute(HttpRequest httpRequest, Function<HttpResponse, T> supplier) throws IOException;

    void settingsPrefaceWritten() throws IOException;

    void setSettingsPromise(ChannelPromise channelPromise);

    void waitForSettings(long value, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException;

    void settingsReceived(Http2Settings http2Settings) throws IOException;

    void responseReceived(Channel channel, Integer streamId, FullHttpResponse fullHttpResponse) throws IOException;

    void pushPromiseReceived(Channel channel, Integer streamId, Integer promisedStreamId, Http2Headers headers);

    void fail(Channel channel, Throwable throwable);

    void inactive(Channel channel);

    void setCookieBox(CookieBox cookieBox);

    CookieBox getCookieBox();

    void setFuture(CompletableFuture<?> future);

    CompletableFuture<?> getFuture();

    Interaction get();

    Interaction get(long value, TimeUnit timeUnit);

    void cancel();

    boolean isFailed();

    Throwable getFailure();

}
