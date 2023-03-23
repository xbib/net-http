package org.xbib.net.http.server.resource;

import org.xbib.net.Resource;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.nio.file.Path;

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
    protected Resource createResource(HttpServerContext httpServerContext) throws IOException {
        return new HtmlTemplateResource(this, httpServerContext);
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
