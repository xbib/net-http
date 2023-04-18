package org.xbib.net.http.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.FileUpload;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.Parameter;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.BaseHttpRequestBuilder;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.xbib.net.http.server.Message;

public class HttpRequestBuilder extends BaseHttpRequestBuilder {

    private static final Logger logger = Logger.getLogger(HttpRequestBuilder.class.getName());

    private FullHttpRequest httpRequest;

    protected ByteBuffer byteBuffer;

    protected CharBuffer charBuffer;

    protected HttpRequestBuilder() {
    }

    public HttpRequestBuilder setHttpRequest(io.netty.handler.codec.http.HttpRequest httpRequest) {
        if (httpRequest != null) {
            setVersion(HttpVersion.valueOf(httpRequest.protocolVersion().text()));
            setMethod(HttpMethod.valueOf(httpRequest.method().name()));
            setRequestURI(httpRequest.uri());
            httpRequest.headers().entries().forEach(e -> addHeader(e.getKey(), e.getValue()));
        }
        return this;
    }

    public HttpRequestBuilder setFullHttpRequest(FullHttpRequest fullHttpRequest) {
        if (fullHttpRequest != null) {
            this.httpRequest = fullHttpRequest;
            setVersion(HttpVersion.valueOf(fullHttpRequest.protocolVersion().text()));
            setMethod(HttpMethod.valueOf(fullHttpRequest.method().name()));
            setRequestURI(fullHttpRequest.uri());
            fullHttpRequest.headers().entries().forEach(e -> addHeader(e.getKey(), e.getValue()));
            if (fullHttpRequest.content() != null) {
                ByteBuf byteBuf = fullHttpRequest.content();
                byte[] bytes = ByteBufUtil.getBytes(byteBuf);
                byteBuffer = ByteBuffer.wrap(bytes);
            }
        }
        return this;
    }

    @Override
    public ByteBuffer getBody() {
        return byteBuffer;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        if (charBuffer == null) {
            charBuffer = byteBuffer != null ? charset.decode(byteBuffer) : null;
        }
        return charBuffer;
    }

    @Override
    public HttpRequestBuilder setAddress(HttpAddress httpAddress) {
        super.setAddress(httpAddress);
        return this;
    }

    @Override
    public HttpRequestBuilder setLocalAddress(InetSocketAddress localAddress) {
        super.setLocalAddress(localAddress);
        return this;
    }

    @Override
    public HttpRequestBuilder setRemoteAddress(InetSocketAddress remoteAddress) {
        super.setRemoteAddress(remoteAddress);
        return this;
    }

    @Override
    public HttpRequestBuilder setBaseURL(URL baseURL) {
        super.setBaseURL(baseURL);
        return this;
    }

    @Override
    public HttpRequestBuilder setBaseURL(HttpAddress httpAddress, String uri, String hostAndPort) {
        super.setBaseURL(httpAddress, uri, hostAndPort);
        return this;
    }

    @Override
    public HttpRequestBuilder setSequenceId(Integer sequenceId) {
        super.setSequenceId(sequenceId);
        return this;
    }

    @Override
    public HttpRequestBuilder setStreamId(Integer streamId) {
        super.setStreamId(streamId);
        return this;
    }

    @Override
    public HttpRequestBuilder setRequestId(Long requestId) {
        super.setRequestId(requestId);
        return this;
    }

    public HttpRequestBuilder addFileUpload(FileUpload fileUpload) throws IOException {
        logger.log(Level.FINE, "add file upload = " + fileUpload);
        Message message = new Message(fileUpload.getContentType(),
                fileUpload.getContentTransferEncoding(),
                fileUpload.getFilename(),
                fileUpload.isInMemory() ? null : fileUpload.getFile().toPath(),
                // can be expensive
                ByteBuffer.wrap(fileUpload.get()));
        super.messages.add(message);
        // we do not need to fileUpload.release() because we let clean up the factory object at the end of channel handling
        return this;
    }

    protected Parameter getParameter() {
        return super.parameter;
    }

    @Override
    public HttpRequest build() {
        return new HttpRequest(this);
    }

    @Override
    public void release() {
        super.release();
        if (httpRequest != null) {
            if (httpRequest.refCnt() > 0) {
                httpRequest.release();
            }
        }
    }
}
