package org.xbib.net.http.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface HttpClient<Req, Resp> extends Closeable {

    <T> CompletableFuture<T> execute(Req request,
                                     Function<Resp, T> supplier) throws IOException;
}
