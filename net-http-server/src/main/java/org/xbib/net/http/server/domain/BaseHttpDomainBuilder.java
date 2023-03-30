package org.xbib.net.http.server.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.service.HttpService;

public class BaseHttpDomainBuilder implements HttpDomainBuilder {

    protected final Set<String> names;

    protected HttpAddress httpAddress;

    protected final Collection<HttpService> httpServices;

    BaseHttpDomainBuilder() {
        this.names = new LinkedHashSet<>();
        this.httpAddress = BaseHttpDomain.getDefaultAddress();
        this.httpServices = new ArrayList<>();
    }

    @Override
    public BaseHttpDomainBuilder addName(String name) {
        this.names.add(name);
        return this;
    }

    @Override
    public BaseHttpDomainBuilder setHttpAddress(HttpAddress httpAddress) throws IOException {
        this.httpAddress = httpAddress;
        names.add(httpAddress.hostAddressAndPort());
        names.add(httpAddress.canonicalHostAndPort());
        return this;
    }

    @Override
    public BaseHttpDomainBuilder addService(HttpService httpService) {
        Objects.requireNonNull(httpService);
        this.httpServices.add(httpService);
        return this;
    }

    @Override
    public BaseHttpDomain build() {
        Objects.requireNonNull(httpAddress);
        return new BaseHttpDomain(this);
    }
}
