package org.xbib.net.http.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.client.HttpClient;
import org.xbib.net.http.client.HttpResponse;

public class NettyHttpClient implements HttpClient<HttpRequest, HttpResponse>, Closeable {

    private static final Logger logger = Logger.getLogger(NettyHttpClient.class.getName());

    private final NettyHttpClientBuilder builder;

    private final EventLoopGroup eventLoopGroup;

    private final Bootstrap bootstrap;

    private final AtomicBoolean closed;

    private final HttpChannelInitializer httpChannelInitializer;

    private final ServiceLoader<HttpChannelInitializer> httpChannelInitializerServiceLoader;

    private Pool pool;

    private final List<Interaction> interactions;

    NettyHttpClient(NettyHttpClientBuilder builder,
                    EventLoopGroup eventLoopGroup,
                    Bootstrap bootstrap) throws IOException {
        this.builder = builder;
        this.eventLoopGroup = eventLoopGroup;
        this.bootstrap = bootstrap;
        this.closed = new AtomicBoolean(false);
        this.httpChannelInitializer = builder.httpChannelInitializer;
        this.httpChannelInitializerServiceLoader = ServiceLoader.load(HttpChannelInitializer.class);
        createBoundedPool(builder.nettyHttpClientConfig, bootstrap);
        this.interactions = new CopyOnWriteArrayList<>();
    }

    public static NettyHttpClientBuilder builder() {
        return new NettyHttpClientBuilder();
    }

    public NettyHttpClient getClient() {
        return this;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public NettyHttpClientConfig getClientConfig() {
        return builder.nettyHttpClientConfig;
    }

    public Pool getPool() {
        return pool;
    }

    public boolean hasPooledNodes() {
        return pool != null && !builder.nettyHttpClientConfig.getPoolNodes().isEmpty();
    }

    public ChannelInitializer<Channel> newChannelInitializer(HttpAddress httpAddress, Interaction interaction) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                interaction.setSettingsPromise(channel.newPromise());
                lookupChannelInitializer(httpAddress)
                        .init(channel, httpAddress, getClient(), builder.nettyCustomizer, interaction);
            }
        };
    }

    /**
     * Execute a HTTP request and return a {@link CompletableFuture}.
     *
     * @param request the request
     * @param supplier the function for the response
     * @param <T> the result of the function for the response
     * @return the completable future
     * @throws IOException if the request fails to be executed.
     */
    @Override
    public <T> CompletableFuture<T> execute(HttpRequest request,
                                            Function<HttpResponse, T> supplier) throws IOException {
        HttpAddress httpAddress = HttpAddress.of(request.getURL(), request.getVersion());
        HttpChannelInitializer initializer = lookupChannelInitializer(httpAddress);
        Interaction interaction = initializer.newInteraction(this, httpAddress);
        interactions.add(interaction);
        return interaction.execute(request, supplier);
    }

    /**
     * Execute HTTP request.
     *
     * @param request the HTTP request
     * @return an interaction
     * @throws IOException if execution fails
     */
    public Interaction execute(HttpRequest request) throws IOException {
        HttpAddress httpAddress = HttpAddress.of(request.getURL(), request.getVersion());
        HttpChannelInitializer initializer = lookupChannelInitializer(httpAddress);
        Interaction interaction = initializer.newInteraction(this, httpAddress);
        CompletableFuture<?> future = new CompletableFuture<>();
        interaction.setFuture(future);
        interactions.add(interaction);
        return interaction.execute(request);
    }

    /**
     * For following redirects, construct a new interaction on a given request URL..
     *
     * @param interaction the previous interaction
     * @param request the new request for continuing the request.
     * @throws IOException if continuation fails
     */
    public void continuation(Interaction interaction, HttpRequest request) throws IOException {
        HttpAddress httpAddress = HttpAddress.of(request.getURL(), request.getVersion());
        HttpChannelInitializer initializer = lookupChannelInitializer(httpAddress);
        Interaction next = initializer.newInteraction(this, httpAddress);
        next.setCookieBox(interaction.getCookieBox());
        next.execute(request);
        next.get();
        closeAndRemove(next);
    }

    /**
     * Retry interaction.
     *
     * @param interaction the interaction to retry
     * @param request the request to retry
     * @throws IOException if retry failed
     */
    public void retry(Interaction interaction, HttpRequest request) throws IOException {
        interaction.execute(request);
        interaction.get();
        closeAndRemove(interaction);
    }

    @Override
    public void close() throws IOException {
        long amount = 15;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        if (closed.compareAndSet(false, true)) {
            try {
                for (Interaction interaction : interactions) {
                    logger.log(Level.FINER, "waiting for unfinshed interaction " + interaction);
                    //interaction.get();
                    interaction.close();
                }
                if (hasPooledNodes()) {
                    logger.log(Level.FINER, "closing pool");
                    pool.close();
                }
                Future<?> future = eventLoopGroup.shutdownGracefully(0L, amount, timeUnit);
                future.await(amount, timeUnit);
                if (future.isSuccess()) {
                    logger.log(Level.FINER, "event loop group closed");
                } else {
                    logger.log(Level.WARNING, "timeout when closing event loop group");
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private void closeAndRemove(Interaction interaction) {
        try {
            interaction.close();
            remove(interaction);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "unable to close interaction: " + e.getMessage(), e);
        }
    }

    void remove(Interaction interaction) {
        interactions.remove(interaction);
    }


    private HttpChannelInitializer lookupChannelInitializer(HttpAddress httpAddress) {
        if (httpChannelInitializer != null || httpAddress == null) {
            return httpChannelInitializer;
        }
        for (HttpChannelInitializer initializer : httpChannelInitializerServiceLoader) {
            if (initializer.supports(httpAddress)) {
                return initializer;
            }
        }
        throw new IllegalStateException("no channel initializer found for address " + httpAddress + ", check service provider");
    }

    private void createBoundedPool(NettyHttpClientConfig nettyHttpClientConfig,
                                   Bootstrap bootstrap) throws IOException {
        List<HttpAddress> nodes = nettyHttpClientConfig.getPoolNodes();
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Integer limit = nettyHttpClientConfig.getPoolNodeConnectionLimit();
        if (limit == null || limit < 1) {
            limit = 1;
        }
        Semaphore semaphore = new Semaphore(limit);
        Integer retries = nettyHttpClientConfig.getRetriesPerPoolNode();
        if (retries == null || retries < 0) {
            retries = 0;
        }
        Integer nodeConnectionLimit = nettyHttpClientConfig.getPoolNodeConnectionLimit();
        if (nodeConnectionLimit == null || nodeConnectionLimit == 0) {
            nodeConnectionLimit = nodes.size();
        }
        this.pool = new BoundedChannelPool(semaphore, nettyHttpClientConfig.getPoolVersion(),
                nodes, retries, nettyHttpClientConfig.getPoolKeySelectorType());
        try {
            this.pool.init(bootstrap, new NettyClientChannelPoolHandler(), nodeConnectionLimit);
        } catch (ConnectException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private class NettyClientChannelPoolHandler implements ChannelPoolHandler {

        @Override
        public void channelReleased(Channel channel) {
        }

        @Override
        public void channelAcquired(Channel channel) {
        }

        @Override
        public void channelCreated(Channel channel) throws IOException {
            HttpAddress httpAddress = channel.attr(Pool.POOL_ATTRIBUTE_KEY).get();
            HttpChannelInitializer initializer = lookupChannelInitializer(httpAddress);
            Interaction interaction = initializer.newInteraction(getClient(), httpAddress);
            initializer.init(channel, httpAddress, getClient(), builder.nettyCustomizer, interaction);
        }
    }
}
