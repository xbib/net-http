package org.xbib.net.http.server.resource;

import java.io.IOException;
import org.xbib.net.Resource;
import org.xbib.net.http.server.route.HttpRouterContext;

public interface HttpServerResource extends Resource {

    void render(HttpRouterContext httpRouterContext) throws IOException;
}
