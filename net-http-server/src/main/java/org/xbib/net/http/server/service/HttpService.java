package org.xbib.net.http.server.service;

import java.util.Collection;
import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.domain.HttpSecurityDomain;

public interface HttpService extends HttpHandler {

    String getPrefix();

    String getPathSpecification();

    Collection<HttpMethod> getMethods();

    Collection<HttpHandler> getHandlers();

    Collection<ParameterDefinition> getParameterDefinitions();

    HttpSecurityDomain getSecurityDomain();
}
