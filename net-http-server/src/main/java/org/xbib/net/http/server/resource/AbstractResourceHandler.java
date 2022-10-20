package org.xbib.net.http.server.resource;

import org.xbib.net.Resource;
import org.xbib.net.URL;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.buffer.DataBufferUtil;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.util.DateTimeUtil;
import org.xbib.net.mime.MimeTypeService;

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

public abstract class AbstractResourceHandler implements HttpHandler {

    protected static final MimeTypeService mimeTypeService = new MimeTypeService();

    private static final Logger logger = Logger.getLogger(AbstractResourceHandler.class.getName());

    public AbstractResourceHandler() {
    }

    protected abstract Resource createResource(HttpServerContext httpServerContext) throws IOException;

    protected abstract boolean isETagResponseEnabled();

    protected abstract boolean isCacheResponseEnabled();

    protected abstract boolean isRangeResponseEnabled();

    protected abstract int getMaxAgeSeconds();

    @Override
    public void handle(HttpServerContext context) throws IOException {
        logger.log(Level.FINE, "handle: before creating resource " + this.getClass().getName());
        Resource resource = createResource(context);
        logger.log(Level.FINE, "handle: resource = " + (resource != null ? resource.getClass().getName() : null));
        if (resource == null || !resource.isExists()) {
            logger.log(Level.FINER, "resource does not exist: " + resource);
            throw new HttpException("resource not found", context, HttpResponseStatus.NOT_FOUND);
        } else if (resource.isDirectory()) {
            logger.log(Level.FINER, "we have a directory request");
            if (!resource.getResourcePath().isEmpty() && !resource.getResourcePath().endsWith("/")) {
                URL url = context.request().getBaseURL();
                String loc = url.resolve(resource.getName() + '/')
                        .mutator()
                        .query(url.getQuery())
                        .fragment(url.getFragment())
                        .build()
                        .toString();
                logger.log(Level.FINER, "client must add a /, external redirect to = " + loc);
                context.response()
                        .addHeader(HttpHeaderNames.LOCATION, loc)
                        .setResponseStatus(HttpResponseStatus.TEMPORARY_REDIRECT)
                        .build().flush(); // flush is important
            } else if (resource.isExistsIndexFile()) {
                // external redirect to default index file in this directory
                logger.log(Level.FINER, "external redirect to default index file in this directory: " + resource.getIndexFileName());
                context.response()
                        .addHeader(HttpHeaderNames.LOCATION, resource.getIndexFileName())
                        .setResponseStatus(HttpResponseStatus.TEMPORARY_REDIRECT)
                        .build().flush(); // flush is important
            } else {
                // send forbidden, we do not allow directory access
                context.response()
                        .setResponseStatus(HttpResponseStatus.FORBIDDEN)
                        .build().flush(); // fluish is important
            }
            context.done();
        } else {
            logger.log(Level.FINE, "handle: generate cacheable resource");
            generateCacheableResource(context, resource);
        }
        logger.log(Level.FINE, "handle: done");
    }

    private void generateCacheableResource(HttpServerContext context, Resource resource) throws IOException {
        // if resource is length of 0, there is nothing to send. Do not send any content,
        if (resource.getLength() == 0) {
            context.response().build().flush();
            return;
        }
        HttpHeaders headers = context.request().getHeaders();
        logger.log(Level.FINE, "before generating resource, the response headers are " + context.response().getHeaders());
        String contentType = resource.getMimeType();
        context.response()
                .addHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
        // heuristic for inline disposition
        String disposition = "inline";
        if (!contentType.startsWith("text") && !contentType.startsWith("image")) {
            String accept = context.request().getHeaders().get(HttpHeaderNames.ACCEPT);
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        }
        if (resource.getBaseName() != null && resource.getSuffix() != null) {
            context.response()
                    .addHeader(HttpHeaderNames.CONTENT_DISPOSITION,
                    disposition + ";filename=\"" + resource.getBaseName() + '.' + resource.getSuffix() + '"');
        }
        long expirationMillis = System.currentTimeMillis() + 1000L * getMaxAgeSeconds();
        if (isCacheResponseEnabled()) {
            context.response()
                    .addHeader(HttpHeaderNames.EXPIRES, DateTimeUtil.formatRfc1123(expirationMillis))
                    .addHeader(HttpHeaderNames.CACHE_CONTROL, "public, max-age=" + getMaxAgeSeconds());
        } else {
            context.response()
                    .addHeader(HttpHeaderNames.EXPIRES, "0")
                    .addHeader(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        }
        boolean sent = false;
        if (isETagResponseEnabled()) {
            Instant lastModifiedInstant = resource.getLastModified();
            String eTag = Long.toHexString(resource.getResourcePath().hashCode() + lastModifiedInstant.toEpochMilli() + resource.getLength());
            Instant ifUnmodifiedSinceInstant = DateTimeUtil.parseDate(headers.get(HttpHeaderNames.IF_UNMODIFIED_SINCE));
            if (ifUnmodifiedSinceInstant != null &&
                    ifUnmodifiedSinceInstant.plusMillis(1000L).isAfter(lastModifiedInstant)) {
                context.response()
                        .setResponseStatus(HttpResponseStatus.PRECONDITION_FAILED)
                        .build().flush();
                return;
            }
            String ifMatch = headers.get(HttpHeaderNames.IF_MATCH);
            if (ifMatch != null && !matches(ifMatch, eTag)) {
                context.response()
                        .setResponseStatus(HttpResponseStatus.PRECONDITION_FAILED)
                        .build().flush();
                return;
            }
            String ifNoneMatch = headers.get(HttpHeaderNames.IF_NONE_MATCH);
            if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
                context.response()
                        .addHeader(HttpHeaderNames.ETAG, eTag)
                        .addHeader(HttpHeaderNames.EXPIRES, DateTimeUtil.formatRfc1123(expirationMillis))
                        .setResponseStatus(HttpResponseStatus.NOT_MODIFIED)
                        .build().flush();
                return;
            }
            Instant ifModifiedSinceInstant = DateTimeUtil.parseDate(headers.get(HttpHeaderNames.IF_MODIFIED_SINCE));
            if (ifModifiedSinceInstant != null &&
                    ifModifiedSinceInstant.plusMillis(1000L).isAfter(lastModifiedInstant)) {
                context.response()
                        .addHeader(HttpHeaderNames.ETAG, eTag)
                        .addHeader(HttpHeaderNames.EXPIRES, DateTimeUtil.formatRfc1123(expirationMillis))
                        .setResponseStatus(HttpResponseStatus.NOT_MODIFIED)
                        .build().flush();
                return;
            }
            context.response()
                    .addHeader(HttpHeaderNames.ETAG, eTag)
                    .addHeader(HttpHeaderNames.LAST_MODIFIED, DateTimeUtil.formatRfc1123(lastModifiedInstant));
            if (isRangeResponseEnabled()) {
                performRangeResponse(context, resource, contentType, eTag, headers);
                sent = true;
            }
        }
        if (!sent) {
            long length = resource.getLength();
            if (length > 0L) {
                String string = Long.toString(resource.getLength());
                context.response()
                        .addHeader(HttpHeaderNames.CONTENT_LENGTH, string);
                send(resource, HttpResponseStatus.OK, contentType, context, 0L, resource.getLength());
            } else {
                send(resource, HttpResponseStatus.OK, contentType, context, 0L, -1L);
            }
        }
        context.done();
    }

    private void performRangeResponse(HttpServerContext context,
                                       Resource resource,
                                       String contentType, String eTag,
                                       HttpHeaders headers) throws IOException {
        long length = resource.getLength();
        context.response().addHeader(HttpHeaderNames.ACCEPT_RANGES, "bytes");
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();
        String range = headers.get(HttpHeaderNames.RANGE);
        if (range != null) {
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                context.response()
                        .addHeader(HttpHeaderNames.CONTENT_RANGE, "bytes */" + length)
                        .setResponseStatus(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .build();
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
                        context.response()
                                .addHeader(HttpHeaderNames.CONTENT_RANGE, "bytes */" + length)
                                .setResponseStatus(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                                .build();
                        return;
                    }
                    ranges.add(new Range(start, end, length));
                }
            }
        }
        if (ranges.isEmpty() || ranges.get(0) == full) {
            context.response()
                    .addHeader(HttpHeaderNames.CONTENT_RANGE, "bytes " + full.start + '-' + full.end + '/' + full.total)
                    .addHeader(HttpHeaderNames.CONTENT_LENGTH, Long.toString(full.length));
            send(resource, HttpResponseStatus.OK, contentType, context, full.start, full.length);
        } else if (ranges.size() == 1) {
            Range r = ranges.get(0);
            context.response()
                    .addHeader(HttpHeaderNames.CONTENT_RANGE, "bytes " + r.start + '-' + r.end + '/' + r.total)
                    .addHeader(HttpHeaderNames.CONTENT_LENGTH, Long.toString(r.length));
            send(resource, HttpResponseStatus.PARTIAL_CONTENT, contentType, context, r.start, r.length);
        } else {
            context.response()
                    .addHeader(HttpHeaderNames.CONTENT_TYPE, "multipart/byteranges; boundary=MULTIPART_BOUNDARY");
            StringBuilder sb = new StringBuilder();
            for (Range r : ranges) {
                try {
                    DataBuffer dataBuffer = readBuffer(context.response(), resource.getURL(), r.start, r.length);
                    sb.append('\n')
                        .append("--MULTIPART_BOUNDARY").append('\n')
                        .append("content-type: ").append(contentType).append('\n')
                        .append("content-range: bytes ").append(r.start).append('-').append(r.end).append('/').append(r.total).append('\n')
                        .append(StandardCharsets.ISO_8859_1.decode(dataBuffer.asByteBuffer()))
                        .append('\n')
                        .append("--MULTIPART_BOUNDARY--").append('\n');
                } catch (URISyntaxException | IOException e) {
                    logger.log(Level.FINEST, e.getMessage(), e);
                }
            }
            context.response()
                    .setResponseStatus(HttpResponseStatus.OK)
                    .setContentType(contentType)
                    .write(CharBuffer.wrap(sb), StandardCharsets.ISO_8859_1);
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
                        HttpServerContext context,
                        long offset, long size) throws IOException {
        if (resource instanceof HttpServerResource) {
            logger.log(Level.FINE, "we have a server resource, render by resource");
            ((HttpServerResource) resource).render(context);
            return;
        }
        URL url = resource.getURL();
        if (url == null) {
            logger.log(Level.WARNING, "url is null, generating not found");
            context.response()
                    .setResponseStatus(HttpResponseStatus.NOT_FOUND)
                    .build();
        } else if (context.request().getMethod() == HttpMethod.HEAD) {
            context.response()
                    .setResponseStatus(HttpResponseStatus.OK)
                    .setContentType(contentType)
                    .build();
        } else {
            if ("file".equals(url.getScheme())) {
                Path path = resource.getPath();
                try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(path)) {
                    send(fileChannel, httpResponseStatus, contentType, context.response(), offset, size);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage() + " path=" + path, e);
                    context.response()
                            .setResponseStatus(HttpResponseStatus.NOT_FOUND)
                            .build();
                }
            } else {
                try (InputStream inputStream = url.openStream()) {
                    send(inputStream, httpResponseStatus, contentType, context.response(), offset, size);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    context.response()
                            .setResponseStatus(HttpResponseStatus.NOT_FOUND)
                            .build();
                }
            }
        }
    }

    protected void send(FileChannel fileChannel,
                        HttpResponseStatus httpResponseStatus,
                        String contentType,
                        HttpResponseBuilder responseBuilder,
                        long offset, long size) throws IOException {
        if (fileChannel == null ) {
            logger.log(Level.WARNING, "file channel is null, generating not found");
            responseBuilder.setResponseStatus(HttpResponseStatus.NOT_FOUND).build();
        } else {
            fileChannel = fileChannel.position(offset);
            try (ReadableByteChannel channel = fileChannel) {
                responseBuilder.setResponseStatus(httpResponseStatus)
                        .setContentType(contentType)
                        .write(DataBufferUtil.readBuffer(responseBuilder.getDataBufferFactory(), channel, size));
            }
        }
    }

    protected void send(InputStream inputStream,
                        HttpResponseStatus httpResponseStatus,
                        String contentType,
                        HttpResponseBuilder responseBuilder,
                        long offset, long size) throws IOException {
        if (inputStream == null) {
            logger.log(Level.WARNING, "inputstream is null, generating not found");
            responseBuilder.setResponseStatus(HttpResponseStatus.NOT_FOUND).build();
        } else {
            long n = inputStream.skip(offset);
            try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
                responseBuilder
                        .setResponseStatus(httpResponseStatus)
                        .setContentType(contentType)
                        .write(DataBufferUtil.readBuffer(responseBuilder.getDataBufferFactory(), channel, size));
            }
        }
    }

    private DataBuffer readBuffer(HttpResponseBuilder responseBuilder, URL url, long offset, long size) throws IOException, URISyntaxException {
        if ("file".equals(url.getScheme())) {
            Path path = Paths.get(url.toURI());
            try (SeekableByteChannel channel = Files.newByteChannel(path)) {
                channel.position(offset);
                return DataBufferUtil.readBuffer(responseBuilder.getDataBufferFactory(), channel, size);
            }
        } else {
            try (InputStream inputStream = url.openStream()) {
                long n = inputStream.skip(offset);
                try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
                    return DataBufferUtil.readBuffer(responseBuilder.getDataBufferFactory(), channel, size);
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
