package org.xbib.net.http.client.netty.secure.http1;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.netty.HttpChannelInitializer;
import org.xbib.net.http.client.netty.Interaction;
import org.xbib.net.http.client.netty.NettyCustomizer;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.http2.Http2Messages;
import org.xbib.net.http.client.netty.http1.Http1Handler;
import org.xbib.net.http.client.netty.TrafficLoggingHandler;
import org.xbib.net.http.client.netty.secure.ClientSecureSocketProvider;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

public class Https1ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Https1ChannelInitializer.class.getName());

    public Https1ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress address) {
        return HttpVersion.HTTP_1_1.equals(address.getVersion()) && address.isSecure();
    }

    @Override
    public Interaction newInteraction(NettyHttpClient client, HttpAddress httpAddress) {
        return new Https1Interaction(client, httpAddress);
    }

    @Override
    public void init(Channel channel,
                     HttpAddress httpAddress,
                     NettyHttpClient nettyHttpClient,
                     NettyCustomizer nettyCustomizer,
                     Interaction interaction) throws IOException {
        NettyHttpsClientConfig nettyHttpClientConfig = (NettyHttpsClientConfig) nettyHttpClient.getClientConfig();
        ChannelPipeline pipeline = channel.pipeline();
        if (nettyHttpClientConfig.isDebug()) {
            pipeline.addLast("client-traffic", new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        int readTimeoutMilllis = nettyHttpClientConfig.getSocketConfig().getReadTimeoutMillis();
        if (readTimeoutMilllis > 0) {
            pipeline.addLast("client-read-timeout", new ReadTimeoutHandler(readTimeoutMilllis / 1000));
        }
        int socketTimeoutMillis = nettyHttpClientConfig.getSocketConfig().getSocketTimeoutMillis();
        if (socketTimeoutMillis > 0) {
            pipeline.addLast("client-idle-timeout", new IdleStateHandler(socketTimeoutMillis / 1000,
                    socketTimeoutMillis / 1000, socketTimeoutMillis / 1000));
        }
        if (nettyHttpClientConfig.getHttpProxyHandler() != null) {
            pipeline.addLast("client-http-proxy", nettyHttpClientConfig.getHttpProxyHandler());
        }
        if (nettyHttpClientConfig.getSocks4ProxyHandler() != null) {
            pipeline.addLast("client-socks4-proxy", nettyHttpClientConfig.getSocks4ProxyHandler());
        }
        if (nettyHttpClientConfig.getSocks5ProxyHandler() != null) {
            Socks5ProxyHandler socks5ProxyHandler = nettyHttpClientConfig.getSocks5ProxyHandler();
            pipeline.addLast("client-socks5-proxy", socks5ProxyHandler);
        }
        configureEncrypted(channel, httpAddress, nettyHttpClient, interaction);
        if (nettyCustomizer != null) {
            nettyCustomizer.afterChannelInitialized(channel);
        }
        if (nettyHttpClientConfig.isDebug()) {
            logger.log(Level.FINE, "HTTP 1.1 secure channel initialized: " +
                    " address=" + httpAddress +
                    " pipeline=" + pipeline.names());
        }
    }

    private void configureEncrypted(Channel channel,
                                    HttpAddress httpAddress,
                                    NettyHttpClient nettyHttpClient,
                                    Interaction interaction) throws IOException {
        NettyHttpsClientConfig nettyHttpClientConfig = (NettyHttpsClientConfig) nettyHttpClient.getClientConfig();
        ChannelPipeline pipeline = channel.pipeline();
        try {
            SslHandler sslHandler = createSslHandler(nettyHttpClientConfig, httpAddress);
            channel.attr(NettyHttpsClientConfig.ATTRIBUTE_KEY_SSL_HANDLER).set(sslHandler);
            pipeline.addLast("client-ssl-handler", sslHandler);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (nettyHttpClientConfig.isProtocolNegotiationEnabled()) {
            ApplicationProtocolNegotiationHandler negotiationHandler =
                    new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {
                        @Override
                        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws IOException {
                            logger.log(Level.FINEST, "configuring pipeline for negotiated protocol " + protocol);
                            if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                                configureHttp2(ctx.channel(), httpAddress, nettyHttpClient, interaction);
                                return;
                            }
                            if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                                configurePlain(ctx.channel(), nettyHttpClient, interaction);
                                return;
                            }
                            ctx.close();
                            throw new IllegalStateException("protocol not accepted: " + protocol);
                        }
                    };
            pipeline.addLast("client-negotiation", negotiationHandler);
        } else {
            configurePlain(channel, nettyHttpClient, interaction);
        }
    }

    private SslHandler createSslHandler(NettyHttpsClientConfig nettyHttpClientConfig,
                                        HttpAddress httpAddress) throws IOException {
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        ClientSecureSocketProvider clientSecureSocketProvider = null;
        for (ClientSecureSocketProvider provider : ServiceLoader.load(ClientSecureSocketProvider.class)) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "trying secure socket provider = " + provider.name());
            }
            if (nettyHttpClientConfig.getSecureSocketProviderName().equals(provider.name())) {
                sslContextBuilder.sslProvider(provider.sslProvider(httpAddress))
                        .ciphers(provider.ciphers(httpAddress), provider.cipherSuiteFilter(httpAddress));
                if (nettyHttpClientConfig.isProtocolNegotiationEnabled()) {
                    sslContextBuilder.applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1));
                }
                if (provider.securityProvider(httpAddress) != null) {
                    Provider p = provider.securityProvider(httpAddress);
                    sslContextBuilder.sslContextProvider(p);
                }
                if (nettyHttpClientConfig.getTrustManagerFactory() != null) {
                    sslContextBuilder.trustManager(nettyHttpClientConfig.getTrustManagerFactory());
                }
                clientSecureSocketProvider = provider;
            }
        }
        InetSocketAddress peer = httpAddress.getInetSocketAddress();
        SslHandler sslHandler = sslContextBuilder.build()
                .newHandler(nettyHttpClientConfig.getByteBufAllocator(), peer.getHostName(), peer.getPort());
        SSLEngine engine = sslHandler.engine();
        SSLParameters params = engine.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        List<SNIServerName> sniServerNames = new ArrayList<>();
        sniServerNames.add(new SNIHostName(httpAddress.getHost())); // only single host_name allowed
        params.setServerNames(sniServerNames);
        engine.setSSLParameters(params);
        switch (nettyHttpClientConfig.getClientAuthMode()) {
            case NEED:
                engine.setNeedClientAuth(true);
                break;
            case WANT:
                engine.setWantClientAuth(true);
                break;
            default:
                break;
        }
        if (clientSecureSocketProvider != null) {
            engine.setEnabledProtocols(clientSecureSocketProvider.protocols(httpAddress));
        }
        if (nettyHttpClientConfig.getSecureProtocolName() != null) {
            String[] enabledProtocols = nettyHttpClientConfig.getSecureProtocolName();
            engine.setEnabledProtocols(enabledProtocols);
            logger.log(Level.FINEST, "TLS: configured protocol = " +
                    Arrays.asList(nettyHttpClientConfig.getSecureProtocolName()));
        }
        sslHandler.setHandshakeTimeoutMillis(nettyHttpClientConfig.getSocketConfig().getSslHandshakeTimeoutMillis());
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "TLS: selected secure socket provider = " +
                    (clientSecureSocketProvider != null ? clientSecureSocketProvider.name() : "<none>"));
            logger.log(Level.FINEST, "TLS:" +
                    " enabled protocols = " + Arrays.asList(engine.getEnabledProtocols()) +
                    " supported protocols = " + Arrays.asList(engine.getSupportedProtocols()) +
                    " application protocol = " + engine.getApplicationProtocol() +
                    " handshake application protocol = " + engine.getHandshakeApplicationProtocol());
            logger.log(Level.FINEST, "TLS: client need auth = " +
                    engine.getNeedClientAuth() + " client want auth = " + engine.getWantClientAuth());
        }
        return sslHandler;
    }

    private void configureHttp2(Channel channel,
                                HttpAddress httpAddress,
                                NettyHttpClient nettyHttpClient,
                                Interaction interaction) throws IOException {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
        ChannelPipeline pipeline = channel.pipeline();
        ChannelInitializer<Channel> initializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                throw new IllegalStateException();
            }
        };
        Http2MultiplexCodecBuilder multiplexCodecBuilder = Http2MultiplexCodecBuilder.forClient(initializer)
                .initialSettings(nettyHttpClientConfig.getHttp2Settings());
        if (nettyHttpClientConfig.isDebug()) {
            multiplexCodecBuilder.frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "client-frame"));
        }
        Http2MultiplexCodec multiplexCodec = multiplexCodecBuilder.autoAckSettingsFrame(true).build();
        pipeline.addLast("client-multiplex", multiplexCodec);
        pipeline.addLast("client-messages", new Http2Messages(interaction));
        // simulate we are ready for HTTP/2
        interaction.settingsReceived(Http2Settings.defaultSettings());
    }

    private void configurePlain(Channel channel,
                                NettyHttpClient nettyHttpClient,
                                Interaction interaction) throws IOException {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("http-client-chunk-writer",
                new ChunkedWriteHandler());
        pipeline.addLast("http-client-codec", new HttpClientCodec(nettyHttpClientConfig.getMaxInitialLineLength(),
                nettyHttpClientConfig.getMaxHeadersSize(), nettyHttpClientConfig.getMaxChunkSize()));
        if (nettyHttpClientConfig.isGzipEnabled()) {
            pipeline.addLast("http-client-decompressor", new HttpContentDecompressor());
        }
        HttpObjectAggregator httpObjectAggregator =
                new HttpObjectAggregator(nettyHttpClientConfig.getMaxContentLength(), false);
        httpObjectAggregator.setMaxCumulationBufferComponents(nettyHttpClientConfig.getMaxCompositeBufferComponents());
        pipeline.addLast("http-client-aggregator", httpObjectAggregator);
        pipeline.addLast("http-client-response", new Http1Handler(interaction));
        interaction.settingsReceived(null);
    }
}
