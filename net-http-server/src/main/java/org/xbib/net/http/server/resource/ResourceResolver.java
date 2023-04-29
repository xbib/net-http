package org.xbib.net.http.server.resource;

import java.io.IOException;
import java.util.List;
import org.xbib.net.Resource;
import org.xbib.net.http.server.route.HttpRouterContext;

public interface ResourceResolver {

    Resource resolveResource(HttpRouterContext httpRouterContext,
                             String template,
                             List<String> indexFiles) throws IOException;
}
