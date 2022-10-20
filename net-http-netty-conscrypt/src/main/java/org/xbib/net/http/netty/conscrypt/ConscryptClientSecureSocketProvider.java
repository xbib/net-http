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
import org.xbib.net.http.client.netty.secure.ClientSecureSocketProvider;

public class ConscryptClientSecureSocketProvider implements ClientSecureSocketProvider {

    static {
        Security.insertProviderAt(Conscrypt.newProviderBuilder()
                .provideTrustManager(true)
                .build(), 1);
    }

    public ConscryptClientSecureSocketProvider() {
    }

    @Override
    public String name() {
        return "CONSCRYPT";
    }

    @Override
    public Provider securityProvider(HttpAddress address) {
        return Conscrypt.newProviderBuilder()
                .provideTrustManager(true)
                .build();
    }

    @Override
    public SslProvider sslProvider(HttpAddress address) {
        return SslProvider.JDK;
    }

    @Override
    public List<String> ciphers(HttpAddress address) {
        return Arrays.asList(((SSLSocketFactory) SSLSocketFactory.getDefault()).getDefaultCipherSuites());
    }

    @Override
    public CipherSuiteFilter cipherSuiteFilter(HttpAddress address) {
        return SupportedCipherSuiteFilter.INSTANCE;
    }

    @Override
    public String[] protocols(HttpAddress address) {
        return new String[] { "TLSv1.2" };
    }
}
