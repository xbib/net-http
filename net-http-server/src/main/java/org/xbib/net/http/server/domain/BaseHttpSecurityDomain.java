package org.xbib.net.http.server.domain;

import java.util.List;
import org.xbib.net.SecurityRealm;
import org.xbib.net.http.server.HttpHandler;

public class BaseHttpSecurityDomain implements HttpSecurityDomain {

    private final BaseHttpSecurityDomainBuilder builder;

    BaseHttpSecurityDomain(BaseHttpSecurityDomainBuilder builder) {
        this.builder = builder;
    }

    public static BaseHttpSecurityDomainBuilder builder() {
        return new BaseHttpSecurityDomainBuilder();
    }

    @Override
    public SecurityRealm getRealm() {
        return builder.securityRealm;
    }

    public List<HttpHandler> getHandlers() {
        return builder.handlers;
    }
}
