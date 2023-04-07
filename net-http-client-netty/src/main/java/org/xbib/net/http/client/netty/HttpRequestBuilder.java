package org.xbib.net.http.client.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.URLBuilder;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.BackOff;
import org.xbib.net.http.client.ExceptionListener;
import org.xbib.net.http.client.HttpResponse;
import org.xbib.net.http.client.Part;
import org.xbib.net.http.client.ResponseListener;
import org.xbib.net.http.client.TimeoutListener;
import org.xbib.net.http.cookie.Cookie;

public class HttpRequestBuilder implements org.xbib.net.http.client.HttpRequestBuilder {

    private static final URL DEFAULT_URL = URL.from("http://localhost");

    private static final String DEFAULT_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

    protected final ByteBufAllocator allocator;

    protected HttpAddress httpAddress;

    protected URL url;

    protected String requestPath;

    protected final Collection<Cookie> cookies;

    protected HttpMethod httpMethod;

    protected HttpHeaders headers;

    protected HttpVersion httpVersion;

    protected final List<String> removeHeaders;

    protected String userAgent;

    protected boolean keepalive;

    protected boolean gzip;

    protected String contentType;

    protected ParameterBuilder parameterBuilder;

    protected ByteBuffer body;

    protected boolean followRedirect;

    protected int maxRedirects;

    protected boolean enableBackOff;

    protected BackOff backOff;

    protected ResponseListener<HttpResponse> responseListener;

    protected ExceptionListener exceptionListener;

    protected TimeoutListener timeoutListener;

    protected long timeoutMillis;

    protected final List<Part> parts;

    protected HttpRequestBuilder() {
        this(ByteBufAllocator.DEFAULT);
    }

    protected HttpRequestBuilder(ByteBufAllocator allocator) {
        this.allocator = allocator;
        this.httpMethod = HttpMethod.GET;
        this.httpVersion = HttpVersion.HTTP_1_1;
        this.userAgent = UserAgent.getUserAgent();
        this.gzip = false;
        this.keepalive = true;
        this.url = DEFAULT_URL;
        this.followRedirect = true;
        this.maxRedirects = 10;
        this.headers = new HttpHeaders();
        this.removeHeaders = new ArrayList<>();
        this.cookies = new HashSet<>();
        this.contentType = DEFAULT_FORM_CONTENT_TYPE;
        this.parameterBuilder = Parameter.builder();
        this.timeoutMillis = 0L;
        this.parts = new ArrayList<>();
    }

    @Override
    public HttpRequestBuilder setAddress(HttpAddress httpAddress) {
        this.httpAddress = httpAddress;
        try {
            this.url = URL.builder()
                    .scheme(httpAddress.isSecure() ? "https" : "http")
                    .host(httpAddress.getInetSocketAddress().getHostString())
                    .port(httpAddress.getInetSocketAddress().getPort())
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.httpVersion = httpAddress.getVersion();
        return this;
    }

    public HttpRequestBuilder setURL(String url) {
        return setURL(URL.from(url));
    }

    @Override
    public HttpRequestBuilder setURL(URL url) {
        this.url = url;
        return this;
    }

    @Override
    public HttpRequestBuilder setRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    public HttpRequestBuilder setMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public HttpRequestBuilder setVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return this;
    }

    public HttpRequestBuilder setVersion(String httpVersion) {
        this.httpVersion = HttpVersion.valueOf(httpVersion);
        return this;
    }

    public HttpRequestBuilder setHeaders(Map<String, String> headers) {
        headers.forEach(this::addHeader);
        return this;
    }

    public HttpRequestBuilder setHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    public HttpRequestBuilder addHeader(String name, String value) {
        this.headers.add(name, value);
        return this;
    }

    public HttpRequestBuilder setHeader(String name, String value) {
        this.headers.set(name, value);
        return this;
    }

    public HttpRequestBuilder removeHeader(String name) {
        removeHeaders.add(name);
        return this;
    }

    public HttpRequestBuilder contentType(String contentType) {
        Objects.requireNonNull(contentType);
        this.contentType = contentType;
        addHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
        return this;
    }

    public HttpRequestBuilder contentType(String contentType, Charset charset) {
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(charset);
        this.contentType = contentType;
        addHeader(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=" + charset.name().toLowerCase());
        return this;
    }

    @Override
    public HttpRequestBuilder setParameterBuilder(ParameterBuilder parameterBuilder) {
        this.parameterBuilder = parameterBuilder;
        return this;
    }

    public HttpRequestBuilder setParameters(Map<String, Object> parameters) {
        parameters.forEach(this::addParameter);
        return this;
    }

    @SuppressWarnings("unchecked")
    public HttpRequestBuilder addParameter(String name, Object value) {
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

    public HttpRequestBuilder addRawParameter(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        parameterBuilder.add(name, value);
        return this;
    }

    public HttpRequestBuilder addBasicAuthorization(String name, String password) {
        String encoding = Base64.getEncoder().encodeToString((name + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.headers.add(HttpHeaderNames.AUTHORIZATION, "Basic " + encoding);
        return this;
    }

    @Override
    public HttpRequestBuilder setBody(ByteBuffer byteBuffer) {
        this.body = byteBuffer;
        return this;
    }

    @Override
    public HttpRequestBuilder addPart(Part part) {
        parts.add(part);
        return this;
    }

    public HttpRequestBuilder addCookie(Cookie cookie) {
        cookies.add(cookie);
        return this;
    }

    public HttpRequestBuilder acceptGzip(boolean gzip) {
        this.gzip = gzip;
        return this;
    }

    public HttpRequestBuilder keepAlive(boolean keepalive) {
        this.keepalive = keepalive;
        return this;
    }

    public HttpRequestBuilder setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    public HttpRequestBuilder setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    public HttpRequestBuilder enableBackOff(boolean enableBackOff) {
        this.enableBackOff = enableBackOff;
        return this;
    }

    public HttpRequestBuilder setBackOff(BackOff backOff) {
        this.backOff = backOff;
        return this;
    }

    public HttpRequestBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public HttpRequestBuilder text(String text) {
        if (text == null) {
            return this;
        }
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(text);
        content(byteBuf, HttpHeaderValues.TEXT_PLAIN);
        return this;
    }

    public HttpRequestBuilder json(String json) {
        if (json == null) {
            return this;
        }
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(json);
        content(byteBuf, HttpHeaderValues.APPLICATION_JSON);
        return this;
    }

    public HttpRequestBuilder xml(String xml) {
        if (xml == null) {
            return this;
        }
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(xml);
        content(byteBuf, "application/xml");
        return this;
    }

    public HttpRequestBuilder content(CharSequence charSequence, CharSequence contentType) {
        if (charSequence == null) {
            return this;
        }
        content(charSequence.toString().getBytes(HttpUtil.getCharset(contentType, StandardCharsets.UTF_8)), contentType.toString());
        return this;
    }

    public HttpRequestBuilder content(CharSequence charSequence, CharSequence contentType, Charset charset) {
        if (charSequence == null) {
            return this;
        }
        content(charSequence.toString().getBytes(charset), contentType.toString());
        return this;
    }

    public HttpRequestBuilder content(byte[] buf, String contentType) {
        if (buf == null) {
            return this;
        }
        content(ByteBuffer.wrap(buf), contentType);
        return this;
    }

    public HttpRequestBuilder content(ByteBuffer content, String contentType) {
        if (content == null) {
            return this;
        }
        setBody(content);
        addHeader(HttpHeaderNames.CONTENT_LENGTH, Long.toString(content.remaining()));
        addHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
        return this;
    }

    public HttpRequestBuilder content(ByteBuffer content) {
        if (content == null) {
            return this;
        }
        this.body = content;
        return this;
    }

    public HttpRequestBuilder setResponseListener(ResponseListener<HttpResponse> responseListener) {
        this.responseListener = responseListener;
        return this;
    }

    public HttpRequestBuilder setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
        return this;
    }

    public HttpRequestBuilder setTimeoutListener(TimeoutListener timeoutListener, long timeoutMillis) {
        this.timeoutListener = timeoutListener;
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public HttpRequest build() {
        this.headers = validateHeaders(headers);
        return new HttpRequest(this);
    }

    protected HttpHeaders validateHeaders(HttpHeaders httpHeaders) {
        Parameter parameter = parameterBuilder.build();
        HttpHeaders validatedHeaders = HttpHeaders.of(headers);
        if (url != null) {
            // add our URI parameters to the URL
            URLBuilder urlBuilder = url.mutator();
            if (requestPath != null) {
                urlBuilder.path(requestPath);
            }
            parameter.forEach(e -> urlBuilder.queryParam(e.getKey(), e.getValue()));
            url = urlBuilder.build();
            String scheme = url.getScheme();
            if (httpVersion.majorVersion() == 2) {
                validatedHeaders.set(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), scheme);
            }
            validatedHeaders.set(HttpHeaderNames.HOST, url.getHostInfo());
        }
        validatedHeaders.set(HttpHeaderNames.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        if (userAgent != null) {
            validatedHeaders.set(HttpHeaderNames.USER_AGENT, userAgent);
        }
        if (gzip) {
            validatedHeaders.set(HttpHeaderNames.ACCEPT_ENCODING, "gzip");
        }
        if (httpMethod.name().equals(HttpMethod.POST.name())) {
            content(parameter.getAsQueryString(), contentType);
        }
        int length = body != null ? body.remaining() : 0;
        if (!validatedHeaders.containsHeader(HttpHeaderNames.CONTENT_LENGTH) && !validatedHeaders.containsHeader(HttpHeaderNames.TRANSFER_ENCODING)) {
            if (length < 0) {
                validatedHeaders.set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
            } else {
                validatedHeaders.set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(length));
            }
        }
        if (!validatedHeaders.containsHeader(HttpHeaderNames.ACCEPT)) {
            validatedHeaders.set(HttpHeaderNames.ACCEPT, "*/*");
        }
        // RFC 2616 Section 14.10
        // "An HTTP/1.1 client that does not support persistent connections MUST include the "close" connection
        // option in every request message."
        if (httpVersion.majorVersion() == 1 && !keepalive) {
            validatedHeaders.set(HttpHeaderNames.CONNECTION, "close");
        }
        // at last, forced removal of unwanted headers
        for (String headerName : removeHeaders) {
            validatedHeaders.remove(headerName);
        }
        return validatedHeaders;
    }
}
