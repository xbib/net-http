package org.xbib.net.http.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedStream;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.server.netty.buffer.NettyDataBuffer;
import org.xbib.net.http.server.netty.buffer.NettyDataBufferFactory;
import org.xbib.net.http.server.netty.http1.HttpPipelinedResponse;
import org.xbib.net.http.server.BaseHttpResponseBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.netty.channel.ChannelFutureListener.CLOSE;

public class HttpResponseBuilder extends BaseHttpResponseBuilder {

    private static final Logger logger = Logger.getLogger(HttpResponseBuilder.class.getName());

    private ChannelHandlerContext ctx;

    HttpResponseBuilder() {
        super();
    }

    @Override
    public void reset() {
        super.reset();
        this.dataBufferFactory = NettyDataBufferFactory.getInstance();
    }

    @Override
    public HttpResponseBuilder setVersion(org.xbib.net.http.HttpVersion version) {
        super.setVersion(version);
        return this;
    }

    @Override
    public HttpResponseBuilder setResponseStatus(org.xbib.net.http.HttpResponseStatus status) {
        super.setResponseStatus(status);
        return this;
    }

    @Override
    public HttpResponseBuilder setContentType(String contentType) {
        super.setContentType(contentType);
        return this;
    }

    @Override
    public HttpResponseBuilder setCharset(Charset charset) {
        super.setCharset(charset);
        return this;
    }

    @Override
    public HttpResponseBuilder setHeader(CharSequence name, String value) {
        super.setHeader(name, value);
        return this;
    }

    @Override
    public HttpResponseBuilder setTrailingHeader(CharSequence name, String value) {
        super.setTrailingHeader(name, value);
        return this;
    }

    @Override
    public HttpResponseBuilder withConnectionCloseHeader(boolean close) {
        super.withConnectionCloseHeader(close);
        return this;
    }

    @Override
    public HttpResponseBuilder shouldClose(boolean shouldClose) {
        super.shouldClose(shouldClose);
        return this;
    }

    @Override
    public HttpResponseBuilder setSequenceId(Integer sequenceId) {
        super.setSequenceId(sequenceId);
        return this;
    }

    @Override
    public HttpResponseBuilder setStreamId(Integer streamId) {
        super.setStreamId(streamId);
        return this;
    }

    @Override
    public HttpResponseBuilder setResponseId(Long responseId) {
        super.setResponseId(responseId);
        return this;
    }

    public HttpResponseBuilder setChannelHandlerContext(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        return this;
    }

    @Override
    public HttpResponse build() {
        Objects.requireNonNull(ctx);
        if (body != null) {
            internalStringWrite(body);
        } else if (charBuffer != null && charset != null) {
            internalBufferWrite(charBuffer, charset);
        } else if (dataBuffer != null) {
            internalBufferWrite(dataBuffer);
        } else if (fileChannel != null) {
            internalFileWrite(fileChannel, bufferSize, true);
        } else if (inputStream != null) {
            internalStreamWrite(inputStream, bufferSize, true);
        } else {
            logger.log(Level.FINEST, "no content, we assume header only");
            flush();
        }
        return new HttpResponse(this);
    }

    public void flush() {
        logger.log(Level.FINEST, "flush netty response");
        this.dataBuffer = NettyDataBufferFactory.getInstance().wrap(Unpooled.buffer(0));
        internalBufferWrite(dataBuffer);
    }

    @Override
    public void release() {
        super.release();
    }

    private void internalStringWrite(String body) {
        internalBufferWrite(dataBufferFactory.wrap(StandardCharsets.UTF_8.encode(body)));
    }

    private void internalBufferWrite(DataBuffer dataBuffer) {
        NettyDataBuffer nettyDataBuffer = (NettyDataBuffer) dataBuffer;
        internalBufferWrite(nettyDataBuffer.getNativeBuffer());
    }

    private void internalBufferWrite(CharBuffer charBuffer, Charset charset) {
        internalBufferWrite(ByteBufUtil.encodeString(ctx.alloc(), charBuffer, charset));
    }

    private void internalBufferWrite(ByteBuf byteBuf) {
        internalBufferWrite(byteBuf, byteBuf.readableBytes(), true);
    }

    private void internalBufferWrite(ByteBuf byteBuf, int length, boolean keepAlive) {
        if (!ctx.channel().isWritable()) {
            logger.log(Level.WARNING, "the channel " + ctx.channel() + " is not writable");
            return;
        }
        super.buildHeaders(length);
        HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(status.code());
        HttpHeaders headers = new DefaultHttpHeaders();
        super.headers.entries().forEach(e -> headers.add(e.getKey(), e.getValue()));
        // fix headers
        if (streamId != null) {
            headers.add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), String.valueOf(streamId));
        }
        if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
            headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
            headers.set(HttpHeaderNames.CONTENT_LENGTH, length);
        }
        HttpHeaders trailingHeaders = new DefaultHttpHeaders();
        super.trailingHeaders.entries().forEach(e -> trailingHeaders.add(e.getKey(), e.getValue()));
        HttpVersion httpVersion = HttpVersion.valueOf(version.text());
        FullHttpResponse fullHttpResponse =
                new DefaultFullHttpResponse(httpVersion, responseStatus, byteBuf.retain(), headers, trailingHeaders);
        ChannelFuture channelFuture;
        if (sequenceId != null) {
            HttpPipelinedResponse httpPipelinedResponse = new HttpPipelinedResponse(fullHttpResponse,
                    ctx.channel().newPromise(), sequenceId);
            channelFuture = ctx.write(httpPipelinedResponse);
        } else {
            channelFuture = ctx.write(fullHttpResponse);
        }
        if (!keepAlive || shouldClose()) {
            logger.log(Level.FINEST, "adding close listener to channel future " + channelFuture);
            channelFuture.addListener(CLOSE);
        }
        logger.log(Level.FINEST, "flush netty ctx");
        ctx.flush();
    }

    private void internalFileWrite(FileChannel fileChannel, int bufferSize, boolean keepAlive) {
        if (!ctx.channel().isWritable()) {
            logger.log(Level.WARNING, "the channel is not writeable: " + ctx.channel());
            return;
        }
        HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(status.code());
        DefaultHttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, responseStatus);
        ctx.write(rsp);
        try {
            ctx.write(new ChunkedNioFile(fileChannel, bufferSize));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ChannelFuture channelFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive || shouldClose()) {
            logger.log(Level.FINEST, "adding close listener to channel future " + channelFuture);
            channelFuture.addListener(CLOSE);
        }
        logger.log(Level.FINEST, "flush netty ctx");
        ctx.flush();
    }

    private void internalStreamWrite(InputStream inputStream, int bufferSize, boolean keepAlive) {
        if (!ctx.channel().isWritable()) {
            logger.log(Level.WARNING, "the channel is not writeable: " + ctx.channel());
            return;
        }
        ByteBuf buffer;
        int count;
        try {
            byte[] chunk = new byte[bufferSize];
            count = inputStream.read(chunk, 0, bufferSize);
            if (count <= 0) {
                return;
            }
            buffer = Unpooled.wrappedBuffer(chunk, 0, count);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return;
        }
        if (count < bufferSize) {
            internalBufferWrite(buffer, count, keepAlive);
        } else {
            // chunked
            super.buildHeaders(0);
            HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(status.code());
            HttpHeaders headers = new DefaultHttpHeaders();
            super.headers.entries().forEach(e -> headers.add(e.getKey(), e.getValue()));
            if (streamId != null) {
                headers.add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), String.valueOf(streamId));
            }
            HttpHeaders trailingHeaders = new DefaultHttpHeaders();
            super.trailingHeaders.entries().forEach(e -> trailingHeaders.add(e.getKey(), e.getValue()));
            DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, responseStatus);
            if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            } else {
                if (keepAlive) {
                    headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
            }
            defaultHttpResponse.headers().set(headers);
            ctx.write(defaultHttpResponse);
            ctx.write(new ChunkedStream(inputStream, bufferSize));
            ChannelFuture channelFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive || shouldClose) {
                logger.log(Level.FINEST, "adding close listener to channel future " + channelFuture);
                channelFuture.addListener(CLOSE);
            }
            logger.log(Level.FINEST, "flush netty ctx");
            ctx.flush();
        }
    }
}
