package org.xbib.net.http.server.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.application.Application;

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

    protected final HttpResponseBuilder responseBuilder;

    protected final Application application;

    protected HtmlTemplateResource(HtmlTemplateResourceHandler templateResourceHandler,
                                   HttpRouterContext httpRouterContext) throws IOException {
        this.templateResourceHandler = templateResourceHandler;
        this.application = httpRouterContext.getAttributes().get(Application.class, "application");
        Objects.requireNonNull(application);
        this.negotiateLocale = application.getSettings().getAsBoolean("negotiateLocale", false);
        this.responseBuilder = httpRouterContext.getAttributes().get(HttpResponseBuilder.class, "response");
        Objects.requireNonNull(responseBuilder);
        Path root = templateResourceHandler.getRoot();
        root = root != null ? root : application.getHome();
        if (root == null) {
            throw new IllegalArgumentException("no home path set for template resource resolving");
        }
        this.resourcePath = httpRouterContext.getRequestBuilder().getRequestPath().substring(1);
        this.path = resourcePath.length() > 0 ? root.resolve(resourcePath) : root;
        logger.log(Level.FINEST, "class = " + getClass().getName() +
                " root = " + root +
                " resource path = " + resourcePath +
                " path = " + path +
                " index file name = " + templateResourceHandler.getIndexFileName());
        this.name = path.getFileName().toString();
        this.baseName = AbstractResourceHandler.basename(name);
        this.suffix = AbstractResourceHandler.suffix(name);
        if (Files.isDirectory(path)) {
            if (getIndexFileName() != null) {
                Path indexPath = path.resolve(templateResourceHandler.getIndexFileName());
                logger.log(Level.FINEST, "resolved to index path = " + indexPath);
                if (Files.exists(indexPath)) {
                    logger.log(Level.FINEST, "index path exists");
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
        logger.log(Level.FINEST, () -> "isExists = " + isExists + " isDirectory = " + isDirectory + " url = " + url);
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
        return templateResourceHandler != null ? templateResourceHandler.getIndexFileName() : null;
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
                ",isDirectory=" + isDirectory + "]";
    }

    @Override
    public void render(HttpRouterContext httpRouterContext) throws IOException {
        // to be overriden
    }

    public String contextPath(HttpRequest request, String rel) {
        return url(request, rel, false);
    }

    public String url(HttpRequest request, String rel) {
        return url(request, rel, true);
    }

    public String url(HttpRequest request, String rel, boolean absolute) {
        String prefix = application.getSettings().get("web.prefix", "/");
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        URL url = request.getServerURL().resolve(prefix).resolve(rel);
        return absolute ? url.toExternalForm() : toOrigin(url);
    }

    public void setResponseStatus(HttpResponseStatus responseStatus) {
        this.responseBuilder.setResponseStatus(responseStatus);
    }

    public void movedPermanently(String url) {
        this.responseBuilder.setResponseStatus(HttpResponseStatus.MOVED_PERMANENTLY);
        this.responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public void found(String url) {
        this.responseBuilder.setResponseStatus(HttpResponseStatus.FOUND);
        this.responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public void seeOther(String url) {
        this.responseBuilder.setResponseStatus(HttpResponseStatus.SEE_OTHER);
        this.responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public void temporaryRedirect(String url) {
        this.responseBuilder.setResponseStatus(HttpResponseStatus.TEMPORARY_REDIRECT);
        this.responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public void notFound() {
        this.responseBuilder.setResponseStatus(HttpResponseStatus.NOT_FOUND);
    }

    public void gone() {
        this.responseBuilder.setResponseStatus(HttpResponseStatus.GONE);
    }

    private static String toOrigin(URL url) {
        StringBuilder sb = new StringBuilder();
        if (url.getPath() != null) {
            sb.append(url.getPath());
        }
        if (url.getQuery() != null) {
            sb.append('?').append(url.getQuery());
        }
        if (url.getFragment() != null) {
            sb.append('#').append(url.getFragment());
        }
        if (sb.length() == 0) {
            sb.append('/');
        }
        return sb.toString();
    }
}
