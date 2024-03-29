package org.xbib.net.http.server;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.server.domain.HttpSecurityDomain;
import org.xbib.net.http.server.service.HttpService;

public interface HttpServiceBuilder {

    HttpServiceBuilder setPrefix(String prefix);

    HttpServiceBuilder setPath(String path);

    HttpServiceBuilder setMethod(HttpMethod... method);

    HttpServiceBuilder setHandler(HttpHandler... handler);

    HttpServiceBuilder setParameterDefinition(ParameterDefinition... parameterDefinition);

    HttpServiceBuilder setSecurityDomain(HttpSecurityDomain securityDomain);

    HttpService build();
}
