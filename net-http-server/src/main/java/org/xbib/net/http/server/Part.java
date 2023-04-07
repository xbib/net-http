package org.xbib.net.http.server;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Part {

    private final String contentType;

    private final String contentTransferEncoding;

    private final String name;

    private final Path path;

    private final ByteBuffer byteBuffer;

    public Part(String contentType,
                String contentTransferEncoding,
                String name,
                Path path,
                ByteBuffer byteBuffer) {
        this.contentType = contentType;
        this.contentTransferEncoding = contentTransferEncoding;
        this.name = name;
        this.path = path;
        this.byteBuffer = byteBuffer;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentTransferEncoding() {
        return contentTransferEncoding;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public String toString() {
        return "Part[name=" + name + ",path=" + path + ",bytebuffer=" + (byteBuffer != null ? UTF_8.decode(byteBuffer) : "") + "]";
    }
}
