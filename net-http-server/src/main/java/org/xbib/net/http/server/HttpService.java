package org.xbib.net.http.server;

import java.util.Collection;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;

public interface HttpService extends HttpHandler {

    String getPathSpecification();

    Collection<HttpMethod> getMethods();

    Collection<HttpHandler> getHandlers();

    Collection<ParameterDefinition> getParameterDefinitions();

    HttpSecurityDomain getSecurityDomain();
}
