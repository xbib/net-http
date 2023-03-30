package org.xbib.net.http.server.resource;

import java.io.IOException;
import java.util.List;
import org.xbib.net.Resource;
import org.xbib.net.http.server.HttpServerContext;

public interface ResourceResolver {

    Resource resolveResource(HttpServerContext httpServerContext,
                             String template,
                             List<String> indexFiles) throws IOException;
}
