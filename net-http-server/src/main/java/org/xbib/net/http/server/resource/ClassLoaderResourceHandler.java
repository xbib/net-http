package org.xbib.net.http.server.resource;

import org.xbib.net.PathNormalizer;
import org.xbib.net.Resource;
import org.xbib.net.URL;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassLoaderResourceHandler extends AbstractResourceHandler {

    private static final Logger logger = Logger.getLogger(ClassLoaderResourceHandler.class.getName());

    private final ClassLoader classLoader;

    private final String prefix;

    public ClassLoaderResourceHandler(ClassLoader classLoader) {
        this(classLoader, null);
    }

    public ClassLoaderResourceHandler(ClassLoader classLoader, String prefix) {
        this.classLoader = classLoader;
        this.prefix = prefix;
    }

    @Override
    protected Resource createResource(HttpServerContext httpServerContext) throws IOException {
        return new ClassLoaderResource(httpServerContext);
    }

    @Override
    protected boolean isETagResponseEnabled() {
        return true;
    }

    @Override
    protected boolean isCacheResponseEnabled() {
        return true;
    }

    @Override
    protected boolean isRangeResponseEnabled() {
        return true;
    }

    @Override
    protected int getMaxAgeSeconds() {
        return 24 * 3600;
    }

    class ClassLoaderResource implements Resource {

        private final Path path;

        private final String name;

        private final String baseName;

        private final String suffix;

        private final String resourcePath;

        private final Instant lastModified;

        private final long length;

        private final String mimeType;

        private URL url;

        ClassLoaderResource(HttpServerContext httpServerContext) throws IOException {
            String requestPath = httpServerContext.request().getRequestPath().substring(1);
            this.mimeType = mimeTypeService.getContentType(requestPath);
            this.resourcePath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            String path = prefix != null ? (prefix.endsWith("/") ? prefix : prefix + "/") : "/";
            path = resourcePath.startsWith("/") ? path + resourcePath.substring(1) : path + resourcePath;
            String normalizedPath = PathNormalizer.normalize(resourcePath);
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            this.path = null;
            this.name = normalizedPath;
            this.baseName = basename(name);
            this.suffix = suffix(name);
            logger.log(Level.FINER, "trying: path=" + path + " classLoader=" + classLoader);
            java.net.URL url = classLoader.getResource(path);
            if (url != null) {
                this.url = URL.create(url.toString());
                URLConnection urlConnection = url.openConnection();
                this.lastModified = Instant.ofEpochMilli(urlConnection.getLastModified());
                this.length = urlConnection.getContentLength();
                logger.log(Level.FINER, "success: path=[" + path +
                        "] -> url=" + url + " lastModified=" + lastModified + "length=" + length);
            } else {
                this.lastModified = Instant.now();
                this.length = 0;
                logger.log(Level.WARNING, "fail: resource not found, url=" + url);
            }
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
        public Instant getLastModified() {
            return lastModified;
        }

        @Override
        public long getLength() {
            return length;
        }

        @Override
        public boolean isExists() {
            return url != null;
        }

        @Override
        public boolean isDirectory() {
            return resourcePath.isEmpty() || resourcePath.endsWith("/");
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String getIndexFileName() {
            return null;
        }

        @Override
        public boolean isExistsIndexFile() {
            return false;
        }

        @Override
        public String toString() {
            return "[ClassLoaderResource:resourcePath=" + resourcePath +
                    ",url=" + url +
                    ",mimeType=" + mimeType +
                    ",lastmodified=" + lastModified +
                    ",length=" + length +
                    ",isDirectory=" + isDirectory() + "]";
        }
    }
}
