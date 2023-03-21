package org.xbib.net.http.server.resource;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.URL;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class HtmlTemplateResource implements HttpServerResource {

    private static final Logger logger = Logger.getLogger(HtmlTemplateResource.class.getName());

    private final HtmlTemplateResourceHandler templateResourceHandler;

    private Path path;

    private final String resourcePath;

    private final Instant lastModified;

    private final long length;

    private final URL url;

    private final boolean isExists;

    private final boolean isDirectory;

    private final boolean isExistsIndexFile;

    private final String name;

    private final String baseName;

    private final String suffix;

    protected final boolean negotiateLocale;

    protected HtmlTemplateResource(HtmlTemplateResourceHandler templateResourceHandler,
                                   HttpServerContext httpServerContext) throws IOException {
        this.templateResourceHandler = templateResourceHandler;
        String indexFileName = templateResourceHandler.getIndexFileName();
        Application application = httpServerContext.attributes().get(Application.class, "application");
        this.negotiateLocale = application.getSettings().getAsBoolean("negotiateLocale", false);
        Path root = templateResourceHandler.getPrefix();
        root = root != null ? root : application.getHome();
        if (root == null) {
            throw new IllegalArgumentException("no home path set for template resource resolving");
        }
        this.resourcePath = httpServerContext.request().getRequestPath().substring(1);
        this.path = resourcePath.length() > 0 ? root.resolve(resourcePath) : root;
        logger.log(Level.FINER, "class = " + getClass().getName() +
                " root = " + root +
                " resource path = " + resourcePath +
                " path = " + path +
                " index file name = " + indexFileName);
        this.name = path.getFileName().toString();
        this.baseName = AbstractResourceHandler.basename(name);
        this.suffix = AbstractResourceHandler.suffix(name);
        if (Files.isDirectory(path)) {
            if (getIndexFileName() != null) {
                Path indexPath = path.resolve(indexFileName);
                logger.log(Level.FINE, "resolved to index path = " + indexPath);
                if (Files.exists(indexPath)) {
                    logger.log(Level.FINE, "index path exists");
                    this.isExistsIndexFile = true;
                    this.path = indexPath;
                    this.isDirectory = false;
                } else {
                    this.isExistsIndexFile = false;
                    this.isDirectory = true;
                }
            } else {
                this.isExistsIndexFile = false;
                this.isDirectory = true;
            }
        } else {
            this.isExistsIndexFile = false;
            this.isDirectory = false;
        }
        this.isExists = Files.exists(path);
        this.url = URL.create(path.toUri().toString());
        logger.log(Level.FINE, "isExists = " + isExists + " isDirectory = " + isDirectory + " url = " + url);
        this.lastModified = isExists ? Files.getLastModifiedTime(path).toInstant() : Instant.now();
        // length will be computed at rendering time
        this.length = -1;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBaseName() {
        return baseName;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public boolean isExists() {
        return isExists;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public String getMimeType() {
        return "text/html; charset=UTF-8";
    }

    @Override
    public String getIndexFileName() {
        return templateResourceHandler.getIndexFileName();
    }

    @Override
    public boolean isExistsIndexFile() {
        return isExistsIndexFile;
    }

    @Override
    public Instant getLastModified() {
        return lastModified;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "[HtmlTemplateResource:resourcePath=" + resourcePath +
                ",path=" + path +
                ",url=" + url +
                ",lastmodified=" + lastModified +
                ",length=" + length +
                ",isExists=" + isExists +
                ",isDirectory=" + isDirectory() + "]";
    }

    @Override
    public void render(HttpServerContext httpServerContext) throws IOException {
        // to be overriden
    }
}
