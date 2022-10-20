package org.xbib.net.http.netty.boringssl;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.security.Provider;
import java.util.List;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.secure.ServerSecureSocketProvider;

public class BoringSSLServerSecureSocketProvider implements ServerSecureSocketProvider {

    public BoringSSLServerSecureSocketProvider() {
    }

    @Override
    public String name() {
        return "BORINGSSL";
    }

    @Override
    public Provider securityProvider(HttpAddress httpAddress) {
        return null;
    }

    @Override
    public SslProvider sslProvider(HttpAddress httpAddress) {
        return SslProvider.OPENSSL;
    }

    @Override
    public List<String> ciphers(HttpAddress httpAddress) {
        if (httpAddress.getVersion().majorVersion() == 2) {
            return Http2SecurityUtil.CIPHERS;
        }
        return List.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256");
    }

    @Override
    public CipherSuiteFilter cipherSuiteFilter(HttpAddress httpAddress) {
        return SupportedCipherSuiteFilter.INSTANCE;
    }

    @Override
    public String[] protocols(HttpAddress httpAddress) {
        return OpenSsl.isAvailable() && OpenSsl.version() <= 0x10101009L ?
                new String[] { "TLSv1.2" } :
                new String[] { "TLSv1.3", "TLSv1.2" };
    }
}
