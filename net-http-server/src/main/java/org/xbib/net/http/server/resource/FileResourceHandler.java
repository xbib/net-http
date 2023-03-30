package org.xbib.net.http.server.resource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.PathNormalizer;
import org.xbib.net.Resource;
import org.xbib.net.URL;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.application.Application;

public class FileResourceHandler extends AbstractResourceHandler {

    private static final Logger logger = Logger.getLogger(FileResourceHandler.class.getName());

    private final String webRoot;

    private final String indexFileName;

    private final String pathNameOfResource;

    public FileResourceHandler() {
        this(null, "index.html", null);
    }

    public FileResourceHandler(String webRoot, String indexFileName, String pathNameOfResource) {
        this.webRoot = webRoot;
        this.indexFileName = indexFileName;
        this.pathNameOfResource = pathNameOfResource;
    }

    @Override
    protected Resource createResource(HttpServerContext httpServerContext) throws IOException {
        String pathSpec = httpServerContext.getAttributes().containsKey("templatePath") ?
                (String) httpServerContext.getAttributes().get("templatePath") :
                pathNameOfResource != null ? pathNameOfResource : httpServerContext.getContextPath();
        if (pathSpec == null || pathSpec.isEmpty()) {
            throw new IllegalArgumentException("path must not be null or empty");
        }
        Resource resource = null;
        if (pathSpec.endsWith("/")) {
            if (indexFileName != null) {
                resource = new FileResource(httpServerContext, pathSpec + indexFileName);
            }
        } else {
            resource = new FileResource(httpServerContext, pathSpec);
            if (resource.isDirectory() && resource.isExistsIndexFile()) {
                logger.log(Level.FINER, "we have a directory and existing index file, so we redirect internally");
                resource = new FileResource(httpServerContext, pathSpec + indexFileName);
            }
        }
        return resource;
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

    protected class FileResource implements Resource {

        private Path path;

        private final String resourcePath;

        private final Instant lastModified;

        private final long length;

        private final URL url;

        private final boolean isDirectory;

        private final boolean isExists;

        private final boolean isExistsIndexFile;

        private final String mimeType;

        private final String name;

        private final String baseName;

        private final String suffix;

        protected FileResource(HttpServerContext httpServerContext, String resourcePath) throws IOException {
            this.resourcePath = resourcePath;
            Application application = httpServerContext.getAttributes().get(Application.class, "application");
            Path root = application.getHome();
            if (root == null) {
                throw new IllegalArgumentException("no home path set for template resource resolving");
            }
            if (resourcePath.startsWith("file:")) {
                this.path = Paths.get(URI.create(resourcePath));
                this.name = path.getFileName().toString();
            } else {
                String normalizedPath = PathNormalizer.normalize(resourcePath);
                if (normalizedPath.startsWith("/")) {
                    normalizedPath = normalizedPath.substring(1);
                }
                this.name = normalizedPath;
                this.path = httpServerContext.resolve(webRoot).resolve(normalizedPath);
            }
            this.mimeType = mimeTypeService.getContentType(resourcePath);
            if (Files.isDirectory(path) && getIndexFileName() != null) {
                // internal redirect to indexFileName
                this.path = path.resolve(indexFileName);
                this.isExistsIndexFile = Files.exists(path);
            } else {
                this.isExistsIndexFile = false;
            }
            this.url = URL.create(path.toUri().toString());
            this.baseName = basename(name);
            this.suffix = suffix(name);
            this.isDirectory = Files.isDirectory(path);
            logger.log(Level.INFO, "resource path =" + resourcePath + " path = " + path + " isDirectory = " + isDirectory);
            this.isExists = Files.exists(path);
            if (isExists) {
                this.lastModified = Files.getLastModifiedTime(path).toInstant();
                this.length = Files.size(path);
            } else {
                this.lastModified = Instant.ofEpochMilli(0L);
                this.length = 0;
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
        public boolean isExists() {
            return isExists;
        }

        @Override
        public boolean isDirectory() {
            return isDirectory;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String getIndexFileName() {
            return indexFileName;
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
            return "[FileResource:resourcePath=" + resourcePath +
                    ",url=" + url +
                    ",mimeType=" + mimeType +
                    ",lastmodified=" + lastModified +
                    ",isexists=" + isExists +
                    ",length=" + length +
                    ",isDirectory=" + isDirectory() + "]";
        }
    }
}
