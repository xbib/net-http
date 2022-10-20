package org.xbib.net.http.client.netty.secure;

import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.SslProvider;
import java.security.Provider;
import org.xbib.net.http.HttpAddress;

public interface ClientSecureSocketProvider {

    String name();

    Provider securityProvider(HttpAddress address);

    SslProvider sslProvider(HttpAddress address);

    Iterable<String> ciphers(HttpAddress address);

    CipherSuiteFilter cipherSuiteFilter(HttpAddress address);

    String[] protocols(HttpAddress address);
}
