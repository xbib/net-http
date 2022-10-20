package org.xbib.net.http.server.resource;

import org.xbib.net.Resource;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;

public interface HttpServerResource extends Resource {

    void render(HttpServerContext httpServerContext) throws IOException;
}
