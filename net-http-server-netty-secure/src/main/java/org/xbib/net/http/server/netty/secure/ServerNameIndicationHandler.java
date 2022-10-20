package org.xbib.net.http.server.netty.secure;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Mapping;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpAddress;

public class ServerNameIndicationHandler extends SniHandler {

    private static final Logger logger = Logger.getLogger(ServerNameIndicationHandler.class.getName());

    private final NettyHttpsServerConfig serverConfig;

    private final HttpAddress httpAddress;

    private SslHandler sslHandler;

    public ServerNameIndicationHandler(NettyHttpsServerConfig serverConfig,
                                       HttpAddress httpAddress,
                                       Mapping<? super String, ? extends SslContext> mapping) {
        super(mapping);
        this.serverConfig = serverConfig;
        this.httpAddress = httpAddress;
    }

    public SslHandler getSslHandler() {
        return sslHandler;
    }

    @Override
    protected SslHandler newSslHandler(SslContext context, ByteBufAllocator allocator) {
        sslHandler = createSslHandler(context, allocator);
        return sslHandler;
    }

    private SslHandler createSslHandler(SslContext sslContext, ByteBufAllocator allocator) {
        SslHandler sslHandler = sslContext.newHandler(allocator);
        SSLEngine engine = sslHandler.engine();
        SSLParameters params = engine.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        params.setEnableRetransmissions(serverConfig.isRetransmissionEnabled());
        params.setCipherSuites(serverConfig.getCipherSuites(httpAddress));
        params.setMaximumPacketSize(serverConfig.getMaximumPacketSize());
        params.setUseCipherSuitesOrder(serverConfig.isUseCipherSuiteOrdered());
        params.setAlgorithmConstraints(serverConfig.getAlgorithmConstraints());
        params.setNeedClientAuth(serverConfig.isNeedsClientAuth());
        params.setNeedClientAuth(serverConfig.isWantsClientAuth());
        engine.setSSLParameters(params);
        String[] protocols = serverConfig.getProtocols(httpAddress);
        logger.log(Level.FINER, () -> "enabled TLS protocols in SSL engine = " + Arrays.asList(protocols));
        engine.setEnabledProtocols(protocols);
        logger.log(Level.FINER, () -> "enabled application protocol negotiator protocols = " +
                sslContext.applicationProtocolNegotiator().protocols());
        return sslHandler;
    }
}
