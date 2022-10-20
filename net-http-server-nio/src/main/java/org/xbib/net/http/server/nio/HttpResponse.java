package org.xbib.net.http.server.nio;

import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.server.BaseHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class HttpResponse extends BaseHttpResponse {

    private final HttpResponseBuilder builder;

    protected HttpResponse(HttpResponseBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    @Override
    public void close() throws IOException {
        builder.internalClose();
    }

    @Override
    public void flush() throws IOException {
        builder.internalFlush();
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

}
