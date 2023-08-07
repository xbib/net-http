package org.xbib.net.http.server.simple;

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
            if (bytes != null) {
                internalWrite(bytes);
            } else if (charBuffer != null && charset != null) {
                internalWrite(charBuffer, charset);
            } else if (dataBuffer != null) {
                internalWrite(dataBuffer);
            } else if (fileChannel != null) {
                internalWrite(fileChannel, bufferSize);
            } else if (inputStream != null) {
                internalWrite(inputStream, bufferSize);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return new HttpResponse(this);
    }

    @Override
    public void release() {
        try {
            internalClose();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    void internalFlush() {
        write(dataBufferFactory.allocateBuffer());
    }

    void internalClose() throws IOException {
        outputStream.close();
    }

    void internalWrite(byte[] bytes) throws IOException {
        if (bytes == null) {
            internalFlush();
        } else {
            internalWrite(dataBufferFactory.wrap(bytes));
        }
    }

    void internalWrite(DataBuffer dataBuffer) throws IOException {
        Objects.requireNonNull(dataBuffer);
        try (WritableByteChannel channel = Channels.newChannel(outputStream)) {
            ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
            long contentLength = byteBuffer.remaining();
            logger.log(Level.INFO, "length = " + contentLength);
            super.buildHeaders(contentLength);
            channel.write(US_ASCII.encode(super.wrapHeaders()));
            while (byteBuffer.hasRemaining()) {
                logger.log(Level.INFO, "channel write byte buffer");
                channel.write(byteBuffer);
            }
        }
    }

    void internalWrite(CharBuffer charBuffer, Charset charset) throws IOException {
        if (charBuffer == null) {
            internalFlush();
        } else {
            Objects.requireNonNull(charset);
            internalWrite(dataBufferFactory.wrap(charset.encode(charBuffer)));
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
