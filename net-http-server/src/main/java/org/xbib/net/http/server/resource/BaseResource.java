package org.xbib.net.http.server.resource;

import org.xbib.net.Resource;
import org.xbib.net.URL;

import java.nio.file.Path;
import java.time.Instant;

public class BaseResource implements Resource {

    private Path path;

    private String name;

    private String baseName;

    private String suffix;

    private String resourcePath;

    private URL url;

    private Instant lastModified;

    private long length;

    private boolean isExists;

    private boolean isDirectory;

    private boolean isExistsIndexFile;

    private String mimeType;

    private String indexFileName;

    public BaseResource() {
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public String getBaseName() {
        return baseName;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    @Override
    public URL getURL() {
        return url;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public Instant getLastModified() {
        return lastModified;
    }

    public void setLength(long length) {
        this.length = length;
    }

    @Override
    public long getLength() {
        return length;
    }

    public void setExists(boolean exists) {
        isExists = exists;
    }

    @Override
    public boolean isExists() {
        return isExists;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public void setIndexFileName(String indexFileName) {
        this.indexFileName = indexFileName;
    }

    @Override
    public String getIndexFileName() {
        return indexFileName;
    }

    public void setExistsIndexFile(boolean isExistsIndexFile) {
        this.isExistsIndexFile = isExistsIndexFile;
    }

    @Override
    public boolean isExistsIndexFile() {
        return isExistsIndexFile;
    }
}
