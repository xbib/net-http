package org.xbib.net.http.server.route;

import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.service.HttpService;

public interface HttpRouterBuilder {

    HttpRouterBuilder setPrefix(String prefix);

    HttpRouterBuilder setHandler(Integer code, HttpHandler httpHandler);

    HttpRouterBuilder addDomain(HttpDomain domain);

    HttpRouterBuilder setRouteResolver(HttpRouteResolver<HttpService> httpRouteResolver);

    HttpRouter build();
}
