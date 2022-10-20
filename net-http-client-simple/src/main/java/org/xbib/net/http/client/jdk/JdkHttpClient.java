package org.xbib.net.http.client.jdk;

import org.xbib.net.http.client.HttpClient;
import org.xbib.net.http.client.HttpRequest;
import org.xbib.net.http.client.HttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class JdkHttpClient implements HttpClient<HttpRequest, HttpResponse> {

    private final JdkHttpClientBuilder builder;

    JdkHttpClient(JdkHttpClientBuilder builder) {
        this.builder = builder;
    }

    @Override
    public <T> CompletableFuture<T> execute(HttpRequest request, Function<HttpResponse, T> supplier) throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
