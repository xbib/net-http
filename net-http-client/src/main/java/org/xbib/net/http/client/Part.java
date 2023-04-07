package org.xbib.net.http.client;

import java.nio.charset.Charset;
import java.nio.file.Path;

public class Part {

    private final String contentType;

    private final String contentTransferEncoding;

    private final String name;

    private final Path path;

    private final Charset charset;

    public Part(String contentType,
                String contentTransferEncoding,
                String name,
                Path path,
                Charset charset) {
        this.contentType = contentType;
        this.contentTransferEncoding = contentTransferEncoding;
        this.name = name;
        this.path = path;
        this.charset = charset;
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

    public Charset getCharset() {
        return charset;
    }
}
