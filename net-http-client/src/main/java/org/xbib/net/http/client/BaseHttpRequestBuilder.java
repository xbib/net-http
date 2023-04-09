package org.xbib.net.http.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.cookie.Cookie;

public abstract class BaseHttpRequestBuilder implements HttpRequestBuilder {

    private static final URL DEFAULT_URL = URL.from("http://localhost");

    private static final String DEFAULT_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

    /**
     * The default value for {@code User-Agent}.
     */
    private static final String DEFAULT_USER_AGENT = String.format("HttpClient/%s (Java/%s/%s)",
            httpClientVersion(), javaVendor(), javaVersion());

    protected HttpAddress httpAddress;

    protected InetSocketAddress localAddress;

    protected InetSocketAddress remoteAddress;

    protected URL url;

    protected String requestPath;

    protected ParameterBuilder parameterBuilder;

    protected Integer sequenceId;

    protected Integer streamId;

    protected Long requestId;

    protected HttpVersion httpVersion;

    protected HttpMethod httpMethod;

    protected HttpHeaders httpHeaders;

    protected ByteBuffer byteBuffer;

    protected List<Message> messages;

    protected boolean followRedirect;

    protected int maxRedirects;

    protected int redirectCount;

    protected final Collection<Cookie> cookies;

    protected String contentType;

    protected String userAgent;

    protected boolean isGzipEnabled;

    protected boolean isKeepAliveEnabled;

    protected boolean isBackoffEnabled;

    protected BackOff backOff;

    protected ResponseListener<HttpResponse> responseListener;

    protected ExceptionListener exceptionListener;

    protected TimeoutListener timeoutListener;

    protected long timeoutMillis;

    protected CompletableFuture<HttpRequest> completableFuture;

    protected BaseHttpRequestBuilder() {
        this.httpMethod = HttpMethod.GET;
        this.httpVersion = HttpVersion.HTTP_1_1;
        this.url = DEFAULT_URL;
        this.contentType = DEFAULT_FORM_CONTENT_TYPE;
        this.userAgent = getUserAgent();
        this.isGzipEnabled = false;
        this.isKeepAliveEnabled = true;
        this.followRedirect = true;
        this.maxRedirects = 10;
        this.parameterBuilder = Parameter.builder();
        this.httpHeaders = new HttpHeaders();
        this.messages = new ArrayList<>();
        this.cookies = new HashSet<>();
    }

    public BaseHttpRequestBuilder setVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return this;
    }

    public BaseHttpRequestBuilder setVersion(String httpVersion) {
        setVersion(HttpVersion.valueOf(httpVersion));
        return this;
    }

    public HttpVersion getVersion() {
        return httpVersion;
    }

    public BaseHttpRequestBuilder setMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public HttpMethod getMethod() {
        return httpMethod;
    }

    public BaseHttpRequestBuilder setHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }

    public BaseHttpRequestBuilder setHeaders(Map<String, String> headers) {
        headers.forEach(this::addHeader);
        return this;
    }

    public BaseHttpRequestBuilder addHeader(String key, String value) {
        this.httpHeaders.add(key, value);
        return this;
    }

    public BaseHttpRequestBuilder contentType(String contentType) {
        Objects.requireNonNull(contentType);
        this.contentType = contentType;
        addHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
        return this;
    }

    public BaseHttpRequestBuilder contentType(String contentType, Charset charset) {
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(charset);
        this.contentType = contentType;
        addHeader(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=" + charset.name().toLowerCase());
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setAddress(HttpAddress httpAddress) {
        this.httpAddress = httpAddress;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setURL(URL url) {
        this.url = url;
        return this;
    }

    public BaseHttpRequestBuilder setURL(String url) {
        return setURL(URL.from(url));
    }

    public URL getUrl() {
        return this.url;
    }

    @Override
    public BaseHttpRequestBuilder setRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setParameterBuilder(ParameterBuilder parameterBuilder) {
        this.parameterBuilder = parameterBuilder;
        return this;
    }

    public BaseHttpRequestBuilder setParameters(Map<String, Object> map) {
        map.forEach(this::addParameter);
        return this;
    }

    @SuppressWarnings("unchecked")
    public BaseHttpRequestBuilder addParameter(String name, Object value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        Collection<Object> collection;
        if (!(value instanceof Collection)) {
            collection = Collections.singletonList(value);
        } else {
            collection = (Collection<Object>) value;
        }
        collection.forEach(v -> parameterBuilder.add(name, v));
        return this;
    }

    public BaseHttpRequestBuilder addRawParameter(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        parameterBuilder.add(name, value);
        return this;
    }

    public BaseHttpRequestBuilder addBasicAuthorization(String name, String password) {
        String encoding = Base64.getEncoder().encodeToString((name + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.httpHeaders.add(HttpHeaderNames.AUTHORIZATION, "Basic " + encoding);
        return this;
    }

    @Override
    public BaseHttpRequestBuilder setBody(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        return this;
    }

    @Override
    public BaseHttpRequestBuilder addMessage(Message message) {
        messages.add(message);
        return this;
    }

    public BaseHttpRequestBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public BaseHttpRequestBuilder setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public BaseHttpRequestBuilder setSequenceId(Integer sequenceId) {
        this.sequenceId = sequenceId;
        return this;
    }

    public BaseHttpRequestBuilder setStreamId(Integer streamId) {
        this.streamId = streamId;
        return this;
    }

    public BaseHttpRequestBuilder setRequestId(Long requestId) {
        this.requestId = requestId;
        return this;
    }

    public BaseHttpRequestBuilder setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    public BaseHttpRequestBuilder setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    public BaseHttpRequestBuilder addCookie(Cookie cookie) {
        cookies.add(cookie);
        return this;
    }

    public BaseHttpRequestBuilder isGzipEnabled(boolean isGzipEnabled) {
        this.isGzipEnabled = isGzipEnabled;
        return this;
    }

    public BaseHttpRequestBuilder isKeepAliveEnabled(boolean keepalive) {
        this.isKeepAliveEnabled = keepalive;
        return this;
    }

    public BaseHttpRequestBuilder isBackOffEnabled(boolean enableBackOff) {
        this.isBackoffEnabled = enableBackOff;
        return this;
    }

    public BaseHttpRequestBuilder setBackOff(BackOff backOff) {
        this.backOff = backOff;
        return this;
    }

    public BaseHttpRequestBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public BaseHttpRequestBuilder text(String text) {
        if (text == null) {
            return this;
        }
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(text);
        content(byteBuf, HttpHeaderValues.TEXT_PLAIN);
        return this;
    }

    public BaseHttpRequestBuilder json(String json) {
        if (json == null) {
            return this;
        }
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(json);
        content(byteBuf, HttpHeaderValues.APPLICATION_JSON);
        return this;
    }

    public BaseHttpRequestBuilder xml(String xml) {
        if (xml == null) {
            return this;
        }
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(xml);
        content(byteBuf, "application/xml");
        return this;
    }

    public BaseHttpRequestBuilder content(CharSequence charSequence, CharSequence contentType, Charset charset) {
        if (charSequence == null) {
            return this;
        }
        content(charSequence.toString().getBytes(charset), contentType.toString());
        return this;
    }

    public BaseHttpRequestBuilder content(byte[] buf, String contentType) {
        if (buf == null) {
            return this;
        }
        content(ByteBuffer.wrap(buf), contentType);
        return this;
    }

    public BaseHttpRequestBuilder content(ByteBuffer content, String contentType) {
        if (content == null) {
            return this;
        }
        setBody(content);
        addHeader(HttpHeaderNames.CONTENT_LENGTH, Long.toString(content.remaining()));
        if (contentType != null) {
            addHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
        }
        return this;
    }

    public BaseHttpRequestBuilder content(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return this;
        }
        this.byteBuffer = byteBuffer;
        return this;
    }

    public BaseHttpRequestBuilder setResponseListener(ResponseListener<HttpResponse> responseListener) {
        this.responseListener = responseListener;
        return this;
    }

    public BaseHttpRequestBuilder setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
        return this;
    }

    public BaseHttpRequestBuilder setTimeoutListener(TimeoutListener timeoutListener, long timeoutMillis) {
        this.timeoutListener = timeoutListener;
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    private static String getUserAgent() {
        return DEFAULT_USER_AGENT;
    }

    private static String httpClientVersion() {
        return Optional.ofNullable(BaseHttpRequestBuilder.class.getPackage().getImplementationVersion())
                .orElse("unknown");
    }

    private static String javaVendor() {
        return Optional.ofNullable(System.getProperty("java.vendor"))
                .orElse("unknown");
    }

    private static String javaVersion() {
        return Optional.ofNullable(System.getProperty("java.version"))
                .orElse("unknown");
    }
}
