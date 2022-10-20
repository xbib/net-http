package org.xbib.net.http.server;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.xbib.net.http.HttpAddress;

public class BaseHttpDomain implements HttpDomain {

    private static final HttpAddress DEFAULT_ADDRESS = HttpAddress.http1("localhost", 8008);

    private final BaseHttpDomainBuilder builder;

    BaseHttpDomain(BaseHttpDomainBuilder builder) {
        this.builder = builder;
    }

    public static HttpAddress getDefaultAddress() {
        return DEFAULT_ADDRESS;
    }

    public static BaseHttpDomainBuilder builder() {
        return new BaseHttpDomainBuilder();
    }

    @Override
    public Set<String> getNames() {
        return builder.names;
    }

    @Override
    public HttpAddress getAddress() {
        return builder.httpAddress;
    }

    @Override
    public Collection<HttpService> getServices() {
        return builder.httpServices;
    }

    @Override
    public String toString() {
        return builder.names + " -> " + builder.httpAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseHttpDomain domain = (BaseHttpDomain) o;
        return Objects.equals(builder.names, domain.builder.names) &&
                Objects.equals(builder.httpAddress, domain.builder.httpAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(builder.names, builder.httpAddress);
    }

    @Override
    public int compareTo(HttpDomain o) {
        return toString().compareTo(o.toString());
    }
}
