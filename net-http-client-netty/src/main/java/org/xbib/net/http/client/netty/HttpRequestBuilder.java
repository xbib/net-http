package org.xbib.net.http.client.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpUtil;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.BaseHttpRequestBuilder;
import org.xbib.net.http.client.ExceptionListener;
import org.xbib.net.http.client.Message;
import org.xbib.net.http.client.ResponseListener;
import org.xbib.net.http.client.TimeoutListener;

public class HttpRequestBuilder extends BaseHttpRequestBuilder {

    protected final ByteBufAllocator allocator;

    protected HttpRequestBuilder() {
        this(ByteBufAllocator.DEFAULT);
    }

    protected HttpRequestBuilder(ByteBufAllocator allocator) {
        super();
        this.allocator = allocator;
        this.userAgent = UserAgent.getUserAgent();
    }

    @Override
    public HttpRequestBuilder setAddress(HttpAddress httpAddress) {
        super.setAddress(httpAddress);
        return this;
    }

    @Override
    public HttpRequestBuilder setURL(URL url) {
        super.setURL(url);
        return this;
    }

    @Override
    public HttpRequestBuilder setURL(String url) {
        super.setURL(url);
        return this;
    }

    @Override
    public HttpRequestBuilder setMethod(HttpMethod httpMethod) {
        super.setMethod(httpMethod);
        return this;
    }

    @Override
    public HttpRequestBuilder setVersion(HttpVersion httpVersion) {
        super.setVersion(httpVersion);
        return this;
    }

    @Override
    public HttpRequestBuilder setVersion(String httpVersion) {
        super.setVersion(httpVersion);
        return this;
    }

    @Override
    public HttpRequestBuilder setRequestPath(String requestPath) {
        super.setRequestPath(requestPath);
        return this;
    }

    @Override
    public HttpRequestBuilder setHeaders(HttpHeaders httpHeaders) {
        super.setHeaders(httpHeaders);
        return this;
    }

    @Override
    public HttpRequestBuilder addHeader(String name, String value) {
        super.addHeader(name, value);
        return this;
    }

    @Override
    public HttpRequestBuilder setParameterBuilder(ParameterBuilder parameterBuilder) {
        this.parameterBuilder = parameterBuilder;
        return this;
    }

    @Override
    public HttpRequestBuilder setParameters(Map<String, Object> map) {
        super.setParameters(map);
        return this;
    }

    @Override
    public HttpRequestBuilder addParameter(String name, Object value) {
        super.addParameter(name, value);
        return this;
    }

    @Override
    public HttpRequestBuilder setBody(ByteBuffer byteBuffer) {
        super.setBody(byteBuffer);
        return this;
    }

    @Override
    public HttpRequestBuilder addMessage(Message message) {
        super.addMessage(message);
        return this;
    }

    @Override
    public HttpRequestBuilder content(ByteBuffer byteBuffer) {
        super.content(byteBuffer);
        return this;
    }

    @Override
    public HttpRequestBuilder content(CharSequence charSequence, CharSequence contentType, Charset charset) {
        super.content(charSequence, contentType, charset);
        return this;
    }

    @Override
    public HttpRequestBuilder setFollowRedirect(boolean followRedirect) {
        super.setFollowRedirect(followRedirect);
        return this;
    }

    @Override
    public HttpRequestBuilder setResponseListener(ResponseListener<org.xbib.net.http.client.HttpResponse> responseListener) {
        super.setResponseListener(responseListener);
        return this;
    }

    @Override
    public HttpRequestBuilder setExceptionListener(ExceptionListener exceptionListener) {
        super.setExceptionListener(exceptionListener);
        return this;
    }

    @Override
    public HttpRequestBuilder setTimeoutListener(TimeoutListener timeoutListener, long timeoutMillis) {
        super.setTimeoutListener(timeoutListener, timeoutMillis);
        return this;
    }

    public HttpRequestBuilder content(CharSequence charSequence, CharSequence contentType) {
        if (charSequence == null) {
            return this;
        }
        // use current content type charset or UTF-8
        content(charSequence.toString().getBytes(HttpUtil.getCharset(contentType, StandardCharsets.UTF_8)), contentType.toString());
        return this;
    }

    public HttpRequest build() {
        return new HttpRequest(this);
    }
}
