package org.xbib.net.http.netty.conscrypt;

import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLSocketFactory;
import org.conscrypt.Conscrypt;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.secure.ServerSecureSocketProvider;

public class ConscryptServerSecureSocketProvider implements ServerSecureSocketProvider {

    static {
        Security.insertProviderAt(Conscrypt.newProviderBuilder()
                .provideTrustManager(true)
                .build(), 1);
    }

    public ConscryptServerSecureSocketProvider() {
    }

    @Override
    public String name() {
        return "CONSCRYPT";
    }

    @Override
    public Provider securityProvider(HttpAddress httpAddress) {
        return Conscrypt.newProviderBuilder()
                .provideTrustManager(true)
                .build();
    }

    @Override
    public SslProvider sslProvider(HttpAddress httpAddress) {
        return SslProvider.JDK;
    }

    @Override
    public List<String> ciphers(HttpAddress httpAddress) {
        return Arrays.asList(((SSLSocketFactory) SSLSocketFactory.getDefault()).getDefaultCipherSuites());
    }

    @Override
    public CipherSuiteFilter cipherSuiteFilter(HttpAddress httpAddress) {
        return SupportedCipherSuiteFilter.INSTANCE;
    }

    @Override
    public String[] protocols(HttpAddress httpAddress) {
        return new String[] { "TLSv1.3", "TLSv1.2" };
    }
}
