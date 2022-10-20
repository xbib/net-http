package org.xbib.net.http.server.netty.secure;

import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.SslProvider;
import java.security.Provider;
import org.xbib.net.http.HttpAddress;

public interface ServerSecureSocketProvider {

    String name();

    Provider securityProvider(HttpAddress httpAddress);

    SslProvider sslProvider(HttpAddress httpAddress);

    Iterable<String> ciphers(HttpAddress httpAddress);

    CipherSuiteFilter cipherSuiteFilter(HttpAddress httpAddress);

    String[] protocols(HttpAddress httpAddress);
}
