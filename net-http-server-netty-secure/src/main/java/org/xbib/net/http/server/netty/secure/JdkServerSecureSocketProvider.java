package org.xbib.net.http.server.netty.secure;

import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.security.Provider;
import java.util.Arrays;
import javax.net.ssl.SSLSocketFactory;
import org.xbib.net.http.HttpAddress;

public class JdkServerSecureSocketProvider implements ServerSecureSocketProvider {

    public JdkServerSecureSocketProvider() {
    }

    @Override
    public String name() {
        return "JDK";
    }

    @Override
    public Provider securityProvider(HttpAddress httpAddress) {
        return null;
    }

    @Override
    public SslProvider sslProvider(HttpAddress httpAddress) {
        return SslProvider.JDK;
    }

    @Override
    public Iterable<String> ciphers(HttpAddress httpAddress) {
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
