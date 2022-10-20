package org.xbib.net.http.server.nio;

import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.server.BaseHttpResponseBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class HttpResponseBuilder extends BaseHttpResponseBuilder {

    private static final Logger logger = Logger.getLogger(HttpResponseBuilder.class.getName());

    protected OutputStream outputStream;

    HttpResponseBuilder() {
        super();
    }

    public HttpResponseBuilder setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        return this;
    }

    @Override
    public HttpResponse build() {
        Objects.requireNonNull(outputStream);
        try {
            if (shouldFlush()) {
                internalFlush();
            }
            if (body != null) {
                internalWrite(body);
            } else if (charBuffer != null && charset != null) {
                internalWrite(charBuffer, charset);
            } else if (dataBuffer != null) {
                internalWrite(dataBuffer);
            } else if (fileChannel != null) {
                internalWrite(fileChannel, bufferSize);
            } else if (inputStream != null) {
                internalWrite(inputStream, bufferSize);
            }
            if (shouldClose()) {
                internalClose();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }        return new HttpResponse(this);
    }

    void internalFlush() throws IOException {
        outputStream.flush();
    }

    void internalClose() throws IOException {
        outputStream.close();
    }

    void internalWrite(String string) throws IOException {
        if (string == null) {
            internalFlush();
        } else {
            write(dataBufferFactory.wrap(StandardCharsets.UTF_8.encode(string)));
        }
    }

    void internalWrite(CharBuffer charBuffer, Charset charset) throws IOException {
        if (charBuffer == null) {
            internalFlush();
        } else {
            Objects.requireNonNull(charset);
            write(dataBufferFactory.wrap(charset.encode(charBuffer)));
        }
    }

    void internalWrite(DataBuffer dataBuffer) throws IOException {
        Objects.requireNonNull(dataBuffer);
        try (WritableByteChannel channel = Channels.newChannel(outputStream)) {
            ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
            int contentLength = byteBuffer.remaining();
            super.buildHeaders(contentLength);
            channel.write(US_ASCII.encode(super.wrapHeaders()));
            while (byteBuffer.hasRemaining()) {
                channel.write(byteBuffer);
            }
        }
    }

    void internalWrite(InputStream inputStream, int bufferSize) throws IOException {
        byte[] b = new byte[bufferSize];
        while (inputStream.available() > 0) {
            int i = inputStream.read(b);
            outputStream.write(b, 0, i);
        }
    }

    void internalWrite(FileChannel fileChannel, int bufferSize) throws IOException {
        try (WritableByteChannel channel = Channels.newChannel(outputStream)) {
            long contentLength = fileChannel.size();
            super.buildHeaders(contentLength);
            channel.write(US_ASCII.encode(super.wrapHeaders()));
            channel.write(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, contentLength));
        }
    }
}
