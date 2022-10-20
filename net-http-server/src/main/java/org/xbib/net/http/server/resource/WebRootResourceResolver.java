package org.xbib.net.http.server.resource;

import org.xbib.net.PathNormalizer;
import org.xbib.net.Resource;
import org.xbib.net.URL;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WebRootResourceResolver implements ResourceResolver {

    private final Path prefix;

    private final String webRoot;

    public WebRootResourceResolver(Path prefix, String webRoot) {
        this.prefix = prefix;
        this.webRoot = webRoot;
    }

    @Override
    public Resource resolveResource(HttpServerContext httpServerContext,
                                    String templateName,
                                    List<String> indexFiles) throws IOException {
        String pathSpec = httpServerContext.attributes().containsKey("forwardedPath") ?
                (String) httpServerContext.attributes().get("forwardedPath") :
                templateName != null ? templateName : httpServerContext.httpRequest().getRequestPath();
        if (pathSpec == null || pathSpec.isEmpty()) {
            throw new IllegalArgumentException("path must not be null or empty");
        }
        Resource resource = null;
        if (pathSpec.endsWith("/")) {
            if (indexFiles != null) {
                for (String indexfile : indexFiles) {
                    resource = createResource(pathSpec + indexfile);
                    if (resource.isExists()) {
                        break;
                    }
                }
            }
        } else {
            resource = createResource(pathSpec);
            if (Files.isDirectory(resource.getPath())) {
                // we need to move temporarily to the directory, and the browser must know about this.
                HttpRequest request = httpServerContext.httpRequest();
                URL url = request.getBaseURL();  //response.server().getPublishURL(request);
                String loc = url.resolve(resource.getName() + '/')
                        .mutator()
                        .query(request.getBaseURL().getQuery())
                        .fragment(request.getBaseURL().getFragment())
                        .build()
                        .toString();
                httpServerContext.response()
                        .setResponseStatus(HttpResponseStatus.TEMPORARY_REDIRECT)
                        .setHeader("location", loc);
            }
        }
        return resource;
    }

    private Resource createResource(String resourcePath) throws IOException {
        Path p;
        BaseResource resource = new BaseResource();
        if (resourcePath.startsWith("file:")) {
            p = Paths.get(URI.create(resourcePath));
            resource.setName(p.getFileName().toString());
            resource.setBaseName(basename(resource.getName()));
            resource.setSuffix(suffix(resource.getName()));
        } else {
            String normalizedPath = PathNormalizer.normalize(resourcePath);
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            resource.setName(normalizedPath);
            resource.setBaseName(basename(normalizedPath));
            resource.setSuffix(suffix(normalizedPath));
            p = prefix.resolve(webRoot).resolve(normalizedPath);
        }
        resource.setPath(p);
        if (Files.isRegularFile(p)) {
            resource.setLastModified(Files.getLastModifiedTime(p).toInstant());
            resource.setLength(Files.size(p));
        }
        return resource;
    }

    private static String basename(String path) {
        return removeSuffix(getFileName(path));
    }

    private static String suffix(String path) {
        return extractSuffix(getFileName(path));
    }

    private static String extractSuffix(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfSuffix(filename);
        return index == -1 ? null : filename.substring(index + 1);
    }

    private static String removeSuffix(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfSuffix(filename);
        return index == -1 ? filename : filename.substring(0, index);
    }

    private static int indexOfSuffix(String filename) {
        if (filename == null) {
            return -1;
        }
        int suffixPos = filename.lastIndexOf('.');
        int lastSeparator = filename.lastIndexOf('/');
        return lastSeparator > suffixPos ? -1 : suffixPos;
    }

    private static String getFileName(String path) {
        if (path == null) {
            return null;
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }

}
