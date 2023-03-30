package org.xbib.net.http.server.resource;

import java.io.IOException;
import org.xbib.net.Resource;
import org.xbib.net.http.server.HttpServerContext;

public interface HttpServerResource extends Resource {

    void render(HttpServerContext httpServerContext) throws IOException;
}
