package org.xbib.net.http.server.domain;

import java.io.IOException;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.service.HttpService;

public interface HttpDomainBuilder {

    HttpDomainBuilder addName(String name);

    HttpDomainBuilder setHttpAddress(HttpAddress httpAddress) throws IOException;

    HttpDomainBuilder addService(HttpService httpService);

    HttpDomain build();
}
