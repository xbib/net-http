package org.xbib.net.http.server;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;

public interface HttpServiceBuilder {

    HttpServiceBuilder setPath(String path);

    HttpServiceBuilder setMethod(HttpMethod... method);

    HttpServiceBuilder setHandler(HttpHandler... handler);

    HttpServiceBuilder setParameterDefinition(ParameterDefinition... parameterDefinition);

    HttpServiceBuilder setSecurityDomain(HttpSecurityDomain securityDomain);

    HttpService build();
}
