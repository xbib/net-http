package org.xbib.net.http.server.route;

import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.domain.HttpDomain;

public interface HttpRouterBuilder {

    HttpRouterBuilder setPrefix(String prefix);

    HttpRouterBuilder setHandler(Integer code, HttpHandler httpHandler);

    HttpRouterBuilder addDomain(HttpDomain domain);

    HttpRouter build();
}
