package org.xbib.net.http.server.resource;

import java.io.IOException;
import java.nio.file.Path;
import org.xbib.net.Resource;
import org.xbib.net.http.server.route.HttpRouterContext;

public class HtmlTemplateResourceHandler extends AbstractResourceHandler {

    protected final Path root;

    protected final String suffix;

    protected final String indexFileName;

    public HtmlTemplateResourceHandler(Path root,
                                       String suffix,
                                       String indexFileName) {
        this.root = root;
        this.suffix = suffix;
        this.indexFileName = indexFileName;
    }

    @Override
    protected Resource createResource(HttpRouterContext httpRouterContext) throws IOException {
        return new HtmlTemplateResource(this, httpRouterContext);
    }

    @Override
    protected boolean isETagResponseEnabled() {
        return false;
    }

    @Override
    protected boolean isCacheResponseEnabled() {
        return false;
    }

    @Override
    protected boolean isRangeResponseEnabled() {
        return false;
    }

    @Override
    protected int getMaxAgeSeconds() {
        return 0;
    }

    public Path getRoot() {
        return root;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getIndexFileName() {
        return indexFileName;
    }
}
