package org.xbib.net.http.server.domain;

import java.util.Arrays;
import java.util.List;
import org.xbib.net.SecurityRealm;
import org.xbib.net.http.server.HttpHandler;

public class BaseHttpSecurityDomainBuilder {

    SecurityRealm securityRealm;

    List<HttpHandler> handlers;

    public BaseHttpSecurityDomainBuilder() {
        this.handlers = List.of();
    }

    public BaseHttpSecurityDomainBuilder setSecurityRealm(SecurityRealm securityRealm) {
        this.securityRealm = securityRealm;
        return this;
    }

    public BaseHttpSecurityDomainBuilder setHandlers(HttpHandler... handlers) {
        this.handlers = Arrays.asList(handlers);
        return this;
    }

    public BaseHttpSecurityDomain build() {
        return new BaseHttpSecurityDomain(this);
    }
}
