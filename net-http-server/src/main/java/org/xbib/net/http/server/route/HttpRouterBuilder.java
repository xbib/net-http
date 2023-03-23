package org.xbib.net.http.server.route;

import org.xbib.net.http.server.HttpDomain;
import org.xbib.net.http.server.HttpHandler;

public interface HttpRouterBuilder {

    HttpRouterBuilder setPrefix(String prefix);

    HttpRouterBuilder setHandler(Integer code, HttpHandler httpHandler);

    HttpRouterBuilder addDomain(HttpDomain domain);

    HttpRouter build();
}
