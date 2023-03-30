package org.xbib.net.http.server.netty;

import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.FullHttpRequest;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpRequestBuilder extends BaseHttpRequestBuilder {

    private static final Logger logger = Logger.getLogger(HttpRequestBuilder.class.getName());

    protected FullHttpRequest fullHttpRequest;

    protected ByteBuffer byteBuffer;

    protected HttpRequestBuilder() {
    }

    public HttpRequestBuilder setFullHttpRequest(FullHttpRequest fullHttpRequest) {
        if (fullHttpRequest != null) {
            // retain request, so we can read the body later without refCnt=0 error
            this.fullHttpRequest = fullHttpRequest.retain();
            setVersion(HttpVersion.valueOf(fullHttpRequest.protocolVersion().text()));
            setMethod(HttpMethod.valueOf(fullHttpRequest.method().name()));
            setRequestURI(fullHttpRequest.uri());
            fullHttpRequest.headers().entries().forEach(e -> addHeader(e.getKey(), e.getValue()));
        }
        return this;
    }

    @Override
    public ByteBuffer getBody() {
        if (byteBuffer != null) {
            return byteBuffer;
        }
        // read all bytes from request into a JDK ByteBuffer. This might be expensive.
        if (fullHttpRequest.content() != null) {
            byteBuffer = ByteBuffer.wrap(ByteBufUtil.getBytes(fullHttpRequest.content()));
        }
        return byteBuffer;
    }

    @Override
    public CharBuffer getBodyAsChars(Charset charset) {
        return fullHttpRequest.content() != null ?
                CharBuffer.wrap(fullHttpRequest.content().toString(charset)) : null;
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

    protected Parameter getParameter() {
        return super.parameter;
    }

    @Override
    public HttpRequest build() {
        return new HttpRequest(this);
    }

    @Override
    public void release() {
        if (fullHttpRequest != null) {
            logger.log(Level.FINER, "releasing retained netty request");
            fullHttpRequest.release();
        }
    }
}
