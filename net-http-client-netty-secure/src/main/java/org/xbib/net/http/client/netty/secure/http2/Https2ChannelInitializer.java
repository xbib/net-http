package org.xbib.net.http.client.netty.secure.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
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
import org.xbib.net.http.client.netty.TrafficLoggingHandler;
import org.xbib.net.http.client.netty.secure.ClientSecureSocketProvider;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

public class Https2ChannelInitializer implements HttpChannelInitializer {

    private static final Logger logger = Logger.getLogger(Https2ChannelInitializer.class.getName());

    public Https2ChannelInitializer() {
    }

    @Override
    public boolean supports(HttpAddress address) {
        return HttpVersion.HTTP_2_0.equals(address.getVersion()) && address.isSecure();
    }

    @Override
    public Interaction newInteraction(NettyHttpClient client, HttpAddress httpAddress) {
        return new Https2Interaction(client, httpAddress);
    }

    @Override
    public void init(Channel channel,
                     HttpAddress httpAddress,
                     NettyHttpClient nettyHttpClient,
                     NettyCustomizer nettyCustomizer,
                     Interaction interaction) {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
        if (nettyHttpClientConfig.isDebug()) {
            channel.pipeline().addLast(new TrafficLoggingHandler(LogLevel.DEBUG));
        }
        configureEncrypted(channel, httpAddress, nettyHttpClient, interaction);
        if (nettyCustomizer != null) {
            nettyCustomizer.afterChannelInitialized(channel);
        }
        if (nettyHttpClientConfig.isDebug()) {
            logger.log(Level.FINE, "HTTP/2 secure channel initialized: address = " + httpAddress +
                    " pipeline = " + channel.pipeline().names());
        }
    }

    private void configureEncrypted(Channel channel,
                                    HttpAddress httpAddress,
                                    NettyHttpClient nettyHttpClient,
                                    Interaction interaction) {
        NettyHttpsClientConfig nettyHttpClientConfig = (NettyHttpsClientConfig) nettyHttpClient.getClientConfig();
        try {
            SslHandler sslHandler = createSslHandler(nettyHttpClientConfig, httpAddress);
            channel.attr(NettyHttpsClientConfig.ATTRIBUTE_KEY_SSL_HANDLER).set(sslHandler);
            channel.pipeline().addLast("client-ssl-handler", sslHandler);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        configurePlain(channel, nettyHttpClient, interaction);
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
                sslContextBuilder.applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2));
                if (provider.securityProvider(httpAddress) != null) {
                    sslContextBuilder.sslContextProvider(provider.securityProvider(httpAddress));
                }
                if (nettyHttpClientConfig.getTrustManagerFactory() != null) {
                    sslContextBuilder.trustManager(nettyHttpClientConfig.getTrustManagerFactory());
                }
                clientSecureSocketProvider = provider;
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "selected secure socket provider = " +
                    (clientSecureSocketProvider != null ? clientSecureSocketProvider.name() : "<none>"));
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
            logger.log(Level.FINEST, "TLS: " +
                    " enabled protocols = " + Arrays.asList(engine.getEnabledProtocols()) +
                    " supported protocols = " + Arrays.asList(engine.getSupportedProtocols()) +
                    " application protocol = " + engine.getApplicationProtocol() +
                    " handshake application protocol = " + engine.getHandshakeApplicationProtocol());
            logger.log(Level.FINEST, "TLS: client need auth = " +
                    engine.getNeedClientAuth() + " client want auth = " + engine.getWantClientAuth());
        }
        return sslHandler;
    }

    private void configurePlain(Channel channel,
                                NettyHttpClient nettyHttpClient,
                                Interaction interaction) {
        NettyHttpClientConfig nettyHttpClientConfig = nettyHttpClient.getClientConfig();
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
        Http2MultiplexCodec multiplexCodec = multiplexCodecBuilder
                .autoAckSettingsFrame(true)
                .autoAckPingFrame(true)
                .gracefulShutdownTimeoutMillis(30000L)
                .build();
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("client-multiplex", multiplexCodec);
        pipeline.addLast("client-messages", new Http2Messages(interaction));
    }
}
