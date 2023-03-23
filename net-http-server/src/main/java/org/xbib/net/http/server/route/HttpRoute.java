package org.xbib.net.http.server.route;

import java.util.Collection;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpMethod;

public interface HttpRoute {

    HttpAddress getHttpAddress();

    Collection<HttpMethod> getHttpMethods();

    String getPrefix();

    String getPath();

    String getEffectivePath();

    boolean matches(ParameterBuilder parameterBuilder, HttpRoute requestedRoute);

    String getSortKey();
}
