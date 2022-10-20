package org.xbib.net.http.server;

import org.xbib.net.http.HttpAddress;

import java.io.IOException;

public interface HttpDomainBuilder {

    HttpDomainBuilder addName(String name);

    HttpDomainBuilder setHttpAddress(HttpAddress httpAddress) throws IOException;

    HttpDomainBuilder addService(HttpService httpService);

    HttpDomain build();
}
