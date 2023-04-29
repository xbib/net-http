package org.xbib.net.http.server.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.Resource;
import org.xbib.net.URL;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.buffer.DataBufferFactory;
import org.xbib.net.buffer.DataBufferUtil;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.mime.MimeTypeService;
import org.xbib.net.util.DateTimeUtil;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public abstract class AbstractResourceHandler implements HttpHandler {

    protected static final MimeTypeService mimeTypeService = new MimeTypeService();

    private static final Logger logger = Logger.getLogger(AbstractResourceHandler.class.getName());

    public AbstractResourceHandler() {
    }

    protected abstract Resource createResource(HttpRouterContext httpRouterContext) throws IOException;

    protected abstract boolean isETagResponseEnabled();

    protected abstract boolean isCacheResponseEnabled();

    protected abstract boolean isRangeResponseEnabled();

    protected abstract int getMaxAgeSeconds();

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        logger.log(Level.FINEST, () -> "handle: before creating resource " + this.getClass().getName());
        Resource resource = createResource(context);
        logger.log(Level.FINEST, () -> "handle: resource = " + (resource != null ? resource.getClass().getName() + " " + resource : null));
        if (resource instanceof HtmlTemplateResource) {
            generateCacheableResource(context, resource);
            return;
        }
        if (resource == null) {
            throw new HttpException("resource not found", context, HttpResponseStatus.NOT_FOUND);
        } else if (resource.isDirectory()) {
            logger.log(Level.FINEST, "we have a directory request");
            if (!resource.getResourcePath().isEmpty() && !resource.getResourcePath().endsWith("/")) {
                URL url = context.getRequestBuilder().getBaseURL();
                String loc = url.resolve(resource.getName() + '/')
                        .mutator()
                        .query(url.getQuery())
                        .fragment(url.getFragment())
                        .build()
                        .toString();
                logger.log(Level.FINEST, "client must add a /, external redirect to = " + loc);
                context.header(HttpHeaderNames.LOCATION, loc)
                        .status(HttpResponseStatus.TEMPORARY_REDIRECT);
            } else if (resource.isExistsIndexFile()) {
                // internal redirect to default index file in this directory
                logger.log(Level.FINEST, "internal redirect to default index file in this directory: " + resource.getIndexFileName());
                generateCacheableResource(context, resource);
            } else {
                // send forbidden, we do not allow directory access
                context.status(HttpResponseStatus.FORBIDDEN);
            }
            context.done();
        } else {
            generateCacheableResource(context, resource);
            context.done();
        }
    }

    private void generateCacheableResource(HttpRouterContext context,
                                           Resource resource) throws IOException {
        // if resource is length of 0, there is nothing to send. Do not send any content
        if (resource.getLength() == 0) {
            logger.log(Level.FINEST, "the resource length is 0, return not found");
            context.status(HttpResponseStatus.NOT_FOUND);
            return;
        }
        HttpHeaders headers = context.getRequestBuilder().getHeaders();
        String contentType = resource.getMimeType();
        context.header(CONTENT_TYPE, contentType);
        // heuristic for inline disposition
        String disposition;
        if (!contentType.startsWith("text") && !contentType.startsWith("image") && !contentType.startsWith("font")) {
            String accept = context.getRequestBuilder().getHeaders().get(HttpHeaderNames.ACCEPT);
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        } else {
            disposition = "inline";
        }
        if (resource.getBaseName() != null && resource.getSuffix() != null) {
            String contentDisposition = disposition + ";filename=\"" + resource.getBaseName() + '.' + resource.getSuffix() + '"';
            logger.log(Level.FINEST, () -> "content type = " + contentType + " content disposition = " + contentDisposition);
            context.header(HttpHeaderNames.CONTENT_DISPOSITION, contentDisposition);
        }
        long expirationMillis = System.currentTimeMillis() + 1000L * getMaxAgeSeconds();
        String expires = DateTimeUtil.formatRfc1123(expirationMillis);
        if (isCacheResponseEnabled()) {
            String cacheControl = "public, max-age=" + getMaxAgeSeconds();
            logger.log(Level.FINEST, () -> "cache response, expires = " + expires + " cache control = " + cacheControl);
            context.header(HttpHeaderNames.EXPIRES, expires)
                    .header(HttpHeaderNames.CACHE_CONTROL, cacheControl);
        } else {
            logger.log(Level.FINEST, () -> "uncached response");
            context.header(HttpHeaderNames.EXPIRES, "0")
                    .header(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        }
        boolean sent = false;
        if (isETagResponseEnabled()) {
            Instant lastModifiedInstant = resource.getLastModified();
            String eTag = Long.toHexString(resource.getResourcePath().hashCode() + lastModifiedInstant.toEpochMilli() + resource.getLength());
            logger.log(Level.FINEST, () -> "eTag = " + eTag);
            Instant ifUnmodifiedSinceInstant = DateTimeUtil.parseDate(headers.get(HttpHeaderNames.IF_UNMODIFIED_SINCE));
            if (ifUnmodifiedSinceInstant != null &&
                    ifUnmodifiedSinceInstant.plusMillis(1000L).isAfter(lastModifiedInstant)) {
                logger.log(Level.FINEST, () -> "precondition failed, lastModified = " + lastModifiedInstant + " ifUnmodifiedSince = " + ifUnmodifiedSinceInstant);
                context.status(HttpResponseStatus.PRECONDITION_FAILED);
                return;
            }
            String ifMatch = headers.get(HttpHeaderNames.IF_MATCH);
            if (ifMatch != null && !matches(ifMatch, eTag)) {
                logger.log(Level.FINEST, () -> "precondition failed, ifMatch = " + ifMatch);
                context.status(HttpResponseStatus.PRECONDITION_FAILED);
                return;
            }
            String ifNoneMatch = headers.get(HttpHeaderNames.IF_NONE_MATCH);
            if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
                logger.log(Level.FINEST, () -> "not modified, eTag = " + eTag);
                context.header(HttpHeaderNames.ETAG, eTag)
                        .status(HttpResponseStatus.NOT_MODIFIED);
                return;
            }
            Instant ifModifiedSinceInstant = DateTimeUtil.parseDate(headers.get(HttpHeaderNames.IF_MODIFIED_SINCE));
            if (ifModifiedSinceInstant != null &&
                    ifModifiedSinceInstant.plusMillis(1000L).isAfter(lastModifiedInstant)) {
                logger.log(Level.FINEST, () -> "not modified (after if-modified-since), eTag = " + eTag);
                context.header(HttpHeaderNames.ETAG, eTag)
                        .status(HttpResponseStatus.NOT_MODIFIED);
                return;
            }
            String lastModified = DateTimeUtil.formatRfc1123(lastModifiedInstant);
            logger.log(Level.FINEST, () -> "sending resource, lastModified = " + lastModified);
            context.header(HttpHeaderNames.ETAG, eTag)
                    .header(HttpHeaderNames.LAST_MODIFIED, lastModified);
            if (isRangeResponseEnabled()) {
                performRangeResponse(context, resource, contentType, eTag, headers);
                sent = true;
            } else {
                logger.log(Level.WARNING, "range response not enabled");
            }
        }
        if (!sent) {
            long length = resource.getLength();
            if (length > 0L) {
                String string = Long.toString(resource.getLength());
                context.header(HttpHeaderNames.CONTENT_LENGTH, string);
                logger.log(Level.FINEST, "length is known = " + resource.getLength());
                send(resource, HttpResponseStatus.OK, contentType, context, 0L, resource.getLength());
            } else {
                logger.log(Level.FINEST, "length is unknown");
                send(resource, HttpResponseStatus.OK, contentType, context, 0L, -1L);
            }
        }
        logger.log(Level.FINEST, "generation done");
    }

    private void performRangeResponse(HttpRouterContext context,
                                      Resource resource,
                                      String contentType,
                                      String eTag,
                                      HttpHeaders headers) throws IOException {
        long length = resource.getLength();
        logger.log(Level.FINEST, "performing range response on resource = " + resource);
        context.header(HttpHeaderNames.ACCEPT_RANGES, "bytes");
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();
        String range = headers.get(HttpHeaderNames.RANGE);
        if (range != null) {
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                context.header(HttpHeaderNames.CONTENT_RANGE, "bytes */" + length)
                        .status(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            String ifRange = headers.get(HttpHeaderNames.IF_RANGE);
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    Instant ifRangeTime = DateTimeUtil.parseDate(ifRange);
                    if (ifRangeTime != null && ifRangeTime.plusMillis(1000).isBefore(resource.getLastModified())) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    long start = sublong(part, 0, part.indexOf('-'));
                    long end = sublong(part, part.indexOf('-') + 1, part.length());
                    if (start == -1L) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1L || end > length - 1) {
                        end = length - 1;
                    }
                    if (start > end) {
                        context.header(HttpHeaderNames.CONTENT_RANGE, "bytes */" + length)
                                .status(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }
                    ranges.add(new Range(start, end, length));
                }
            }
        }
        if (ranges.isEmpty() || ranges.get(0) == full) {
            context.header(HttpHeaderNames.CONTENT_RANGE, "bytes " + full.start + '-' + full.end + '/' + full.total)
                    .header(HttpHeaderNames.CONTENT_LENGTH, Long.toString(full.length));
            send(resource, HttpResponseStatus.OK, contentType, context, full.start, full.length);
        } else if (ranges.size() == 1) {
            Range r = ranges.get(0);
            context.header(HttpHeaderNames.CONTENT_RANGE, "bytes " + r.start + '-' + r.end + '/' + r.total)
                    .header(HttpHeaderNames.CONTENT_LENGTH, Long.toString(r.length));
            send(resource, HttpResponseStatus.PARTIAL_CONTENT, contentType, context, r.start, r.length);
        } else {
            context.header(CONTENT_TYPE, "multipart/byteranges; boundary=MULTIPART_BOUNDARY");
            StringBuilder sb = new StringBuilder();
            for (Range r : ranges) {
                try {
                    DataBuffer dataBuffer = readBuffer(context.getDataBufferFactory(), resource.getURL(), r.start, r.length);
                    sb.append('\n')
                        .append("--MULTIPART_BOUNDARY").append('\n')
                        .append("content-type: ").append(contentType).append('\n')
                        .append("content-range: bytes ").append(r.start).append('-').append(r.end).append('/').append(r.total).append('\n')
                        .append(StandardCharsets.ISO_8859_1.decode(dataBuffer.asByteBuffer()))
                        .append('\n')
                        .append("--MULTIPART_BOUNDARY--").append('\n');
                    dataBuffer.release();
                } catch (URISyntaxException | IOException e) {
                    logger.log(Level.FINEST, e.getMessage(), e);
                }
            }
            context.status(HttpResponseStatus.OK)
                    .header(CONTENT_TYPE, contentType)
                    .body(CharBuffer.wrap(sb), StandardCharsets.ISO_8859_1);
        }
    }

    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return substring.length() > 0 ? Long.parseLong(substring) : -1;
    }

    protected void send(Resource resource,
                        HttpResponseStatus httpResponseStatus,
                        String contentType,
                        HttpRouterContext context,
                        long offset,
                        long size) throws IOException {
        if (resource instanceof HttpServerResource) {
            logger.log(Level.FINEST, "let server resource render");
            ((HttpServerResource) resource).render(context);
            return;
        }
        URL url = resource.getURL();
        logger.log(Level.FINEST, "sending URL = " + url + " offset = " + offset + " size = " + size);
        if (url == null) {
            context.status(HttpResponseStatus.NOT_FOUND);
        } else if (context.getRequestBuilder().getMethod() == HttpMethod.HEAD) {
            logger.log(Level.FINEST, "HEAD request, do not send body");
            context.status(HttpResponseStatus.OK)
                    .header(CONTENT_TYPE, contentType);
        } else {
            if ("file".equals(url.getScheme())) {
                Path path = resource.getPath();
                try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(path)) {
                    send(fileChannel, httpResponseStatus, contentType, context, offset, size);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage() + " path=" + path, e);
                    context.status(HttpResponseStatus.NOT_FOUND);
                }
            } else {
                try (InputStream inputStream = url.openStream()) {
                    if (inputStream != null) {
                        send(inputStream, httpResponseStatus, contentType, context, offset, size);
                    } else {
                        logger.log(Level.WARNING, "input stream is null, url = " + url);
                        context.status(HttpResponseStatus.NOT_FOUND);
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage() + " url=" + url, e);
                    context.status(HttpResponseStatus.NOT_FOUND);
                }
            }
        }
    }

    protected void send(FileChannel fileChannel,
                        HttpResponseStatus httpResponseStatus,
                        String contentType,
                        HttpRouterContext context,
                        long offset, long size) throws IOException {
        if (fileChannel == null ) {
            logger.log(Level.WARNING, "file channel is null, generating not found");
            context.status(HttpResponseStatus.NOT_FOUND);
        } else {
            fileChannel = fileChannel.position(offset);
            try (ReadableByteChannel channel = fileChannel) {
                DataBuffer dataBuffer = DataBufferUtil.readBuffer(context.getDataBufferFactory(), channel, size);
                context.status(httpResponseStatus)
                        .header(CONTENT_TYPE, contentType)
                        .body(dataBuffer);
            }
        }
    }

    protected void send(InputStream inputStream,
                        HttpResponseStatus httpResponseStatus,
                        String contentType,
                        HttpRouterContext context,
                        long offset,
                        long size) throws IOException {
        if (inputStream == null) {
            logger.log(Level.WARNING, "inputstream is null, generating not found");
            context.status(HttpResponseStatus.NOT_FOUND);
        } else {
            long n = inputStream.skip(offset);
            try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
                DataBuffer dataBuffer = DataBufferUtil.readBuffer(context.getDataBufferFactory(), channel, size);
                context.status(httpResponseStatus)
                        .header(CONTENT_TYPE, contentType)
                        .body(dataBuffer);
            }
        }
    }

    private DataBuffer readBuffer(DataBufferFactory factory, URL url, long offset, long size)
            throws IOException, URISyntaxException {
        if ("file".equals(url.getScheme())) {
            Path path = Paths.get(url.toURI());
            try (SeekableByteChannel channel = Files.newByteChannel(path)) {
                channel.position(offset);
                return DataBufferUtil.readBuffer(factory, channel, size);
            }
        } else {
            try (InputStream inputStream = url.openStream()) {
                long n = inputStream.skip(offset);
                try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
                    return DataBufferUtil.readBuffer(factory, channel, size);
                }
            }
        }
    }

    protected static String basename(String path) {
        return removeSuffix(getFileName(path));
    }

    protected static String suffix(String path) {
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

    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*([,;])\\s*");
        Arrays.sort(acceptValues);
        boolean b1 = Arrays.binarySearch(acceptValues, toAccept) > -1;
        boolean b2 = Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1;
        boolean b3 = Arrays.binarySearch(acceptValues, "*/*") > -1;
        return b1 || b2 || b3;
    }

    static class Range {
        long start;
        long end;
        long length;
        long total;

        Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }
}
