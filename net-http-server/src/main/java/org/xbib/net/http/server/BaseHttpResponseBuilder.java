package org.xbib.net.http.server;

import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.datastructures.common.Pair;
import org.xbib.net.Attributes;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.buffer.DataBufferFactory;
import org.xbib.net.buffer.DefaultDataBufferFactory;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.cookie.Cookie;
import org.xbib.net.http.server.auth.BaseAttributes;
import org.xbib.net.http.server.cookie.CookieEncoder;

public abstract class BaseHttpResponseBuilder implements HttpResponseBuilder {

    private static final Logger logger = Logger.getLogger(BaseHttpResponseBuilder.class.getName());

    /**
     * A space character for constructing the response.
     */
    private static final Character SPACE = ' ';


    /**
     * A colon character for header construction.
     */
    private static final Character COLON = ':';

    private static final String CRLF = "\r\n";

    protected DataBufferFactory dataBufferFactory;

    protected HttpHeaders headers;

    protected HttpHeaders trailingHeaders;

    protected HttpVersion version;

    protected HttpResponseStatus status;

    /**
     * For getting the server name.
     */
    protected HttpServerConfig httpServerConfig;

    protected boolean withConnectionCloseHeader;

    protected boolean shouldClose;

    protected Integer sequenceId;

    protected Integer streamId;

    protected Long responseId;

    protected String contentType;

    protected Charset charset;

    protected String body;

    protected CharBuffer charBuffer;

    protected DataBuffer dataBuffer;

    protected InputStream inputStream;

    protected FileChannel fileChannel;

    protected int bufferSize;

    protected Long length;

    protected boolean done;

    /**
     * Attributes for applications that want to save data in the response.
     */
    protected Attributes attributes;

    protected BaseHttpResponseBuilder() {
        reset();
    }

    public void reset() {
        this.version = HttpVersion.HTTP_1_1;
        this.status = null; // must be undefined here
        this.headers = new HttpHeaders();
        this.trailingHeaders = new HttpHeaders();
        this.contentType = HttpHeaderValues.APPLICATION_OCTET_STREAM;
        this.dataBufferFactory = DefaultDataBufferFactory.getInstance();
        this.withConnectionCloseHeader = false; // tell client we want to keep the connection alive
        this.attributes = new BaseAttributes();
    }

    @Override
    public BaseHttpResponseBuilder setDataBufferFactory(DataBufferFactory dataBufferFactory) {
        this.dataBufferFactory = dataBufferFactory;
        return this;
    }

    @Override
    public DataBufferFactory getDataBufferFactory() {
        return dataBufferFactory;
    }

    @Override
    public BaseHttpResponseBuilder setVersion(HttpVersion version) {
        if (done) {
            return this;
        }
        this.version = version;
        return this;
    }

    @Override
    public BaseHttpResponseBuilder setResponseStatus(HttpResponseStatus status) {
        if (done) {
            return this;
        }
        if (this.status == null) {
            this.status = status;
        } else {
            logger.log(Level.WARNING, "ignoring status = " + status + " because already set: " + this.status);
        }
        return this;
    }

    @Override
    public HttpResponseStatus getResponseStatus() {
        return status;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public BaseHttpResponseBuilder setHeader(CharSequence name, String value) {
        if (done) {
            return this;
        }
        if (HttpHeaderNames.CONTENT_TYPE.equalsIgnoreCase(name.toString())) {
            setContentType(value);
        }
        if (headers.containsHeader(name)) {
            logger.log(Level.WARNING, "header already exist: " + headers.get(name) + " overwriting with " + value);
        }
        headers.set(name, value);
        return this;
    }

    @Override
    public BaseHttpResponseBuilder addHeader(CharSequence name, String value) {
        if (done) {
            return this;
        }
        if (headers.containsHeader(name)) {
            logger.log(Level.WARNING, "header already exist: " + headers.get(name) + " adding " + value);
        }
        headers.add(name, value);
        return this;
    }

    @Override
    public BaseHttpResponseBuilder setTrailingHeader(CharSequence name, String value) {
        if (done) {
            return this;
        }
        trailingHeaders.set(name, value);
        return this;
    }

    @Override
    public BaseHttpResponseBuilder setContentType(String contentType) {
        if (done) {
            return this;
        }
        this.contentType = contentType;
        return this;
    }

    @Override
    public BaseHttpResponseBuilder setCharset(Charset charset) {
        if (done) {
            return this;
        }
        this.charset = charset;
        return this;
    }

    @Override
    public BaseHttpResponseBuilder withConnectionCloseHeader(boolean withConnectionCloseHeader) {
        if (done) {
            return this;
        }
        this.withConnectionCloseHeader = withConnectionCloseHeader;
        return this;
    }

    @Override
    public boolean withConnectionCloseHeader() {
        return withConnectionCloseHeader;
    }

    @Override
    public HttpResponseBuilder shouldClose(boolean shouldClose) {
        if (done) {
            return this;
        }
        this.shouldClose = shouldClose;
        return this;
    }

    @Override
    public boolean shouldClose() {
        return shouldClose;
    }

    @Override
    public BaseHttpResponseBuilder setSequenceId(Integer sequenceId) {
        if (done) {
            return this;
        }
        this.sequenceId = sequenceId;
        return this;
    }

    @Override
    public BaseHttpResponseBuilder setStreamId(Integer streamId) {
        if (done) {
            return this;
        }
        this.streamId = streamId;
        return this;
    }

    @Override
    public BaseHttpResponseBuilder setResponseId(Long responseId) {
        if (done) {
            return this;
        }
        this.responseId = responseId;
        return this;
    }

    @Override
    public BaseHttpResponseBuilder write(String body) {
        if (body != null && this.body == null) {
            this.body = body;
        } else {
            logger.log(Level.WARNING, "cannot write more than one body");
        }
        return this;
    }

    @Override
    public BaseHttpResponseBuilder write(CharBuffer charBuffer, Charset charset) {
        if (charBuffer != null && this.charBuffer == null) {
            this.charBuffer = charBuffer;
            this.charset = charset;
        } else {
            logger.log(Level.WARNING, "cannot write more than one CharBuffer");
        }
        return this;
    }

    @Override
    public BaseHttpResponseBuilder write(DataBuffer dataBuffer) {
        if (dataBuffer != null && this.dataBuffer == null) {
            this.dataBuffer = dataBuffer;
        } else {
            logger.log(Level.WARNING, "cannot write more than one DataBuffer");
        }
        return this;
    }

    @Override
    public BaseHttpResponseBuilder write(InputStream inputStream, int bufferSize) {
        if (inputStream != null && this.inputStream == null) {
            this.inputStream = inputStream;
            this.bufferSize = bufferSize;
        } else {
            logger.log(Level.WARNING, "cannot write more than one InputStream");
        }
        return this;
    }

    @Override
    public BaseHttpResponseBuilder write(FileChannel fileChannel, int bufferSize) {
        if (fileChannel != null && this.fileChannel == null) {
            this.fileChannel = fileChannel;
            this.bufferSize = bufferSize;
        } else {
            logger.log(Level.WARNING, "cannot write more than one FileChannel");
        }
        return this;
    }

    @Override
    public BaseHttpResponseBuilder addCookie(Cookie cookie) {
        if (done) {
            return this;
        }
        Objects.requireNonNull(cookie);
        headers.add(HttpHeaderNames.SET_COOKIE, CookieEncoder.STRICT.encode(cookie));
        return this;
    }

    @Override
    public Long getLength() {
        return length != null ? length :
                headers.containsHeader(HttpHeaderNames.CONTENT_LENGTH) ?
                Long.parseLong(headers.get(HttpHeaderNames.CONTENT_LENGTH)) : null;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public abstract HttpResponse build();

    @Override
    public void done() {
        this.done = true;
        logger.log(Level.FINEST, "done");
    }

    @Override
    public void release() {
        if (dataBuffer != null) {
            dataBuffer.release();
        }
    }

    public void buildHeaders(long contentLength) {
        this.length = contentLength;
        if (!headers.containsHeader(HttpHeaderNames.CONTENT_TYPE)) {
            if (contentType == null) {
                contentType = HttpHeaderValues.APPLICATION_OCTET_STREAM;
            }
            if (!contentType.contains("charset=") && charset != null) {
                contentType = contentType + "; charset=" + charset.name();
            }
            headers.add(HttpHeaderNames.CONTENT_TYPE, contentType);
        }
        if (status == null) {
            logger.log(Level.WARNING, "no status code set by handlers, assuming OK");
            status = HttpResponseStatus.OK;
        }
        if (status.code() >= 200 && status.code() != 204) {
            if (!headers.containsHeader(HttpHeaderNames.CONTENT_LENGTH)) {
                headers.add(HttpHeaderNames.CONTENT_LENGTH, Long.toString(contentLength));
            }
        }
        if (withConnectionCloseHeader) {
            headers.add(HttpHeaderNames.CONNECTION, "close");
        }
        if (!headers.containsHeader(HttpHeaderNames.DATE)) {
            headers.add(HttpHeaderNames.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        }
        if (httpServerConfig != null && httpServerConfig.getServerName() != null) {
            headers.add(HttpHeaderNames.SERVER, httpServerConfig.getServerName());
        }
        logger.log(Level.FINEST, () -> "headers: status = " + status + " headers = " + headers);
    }

    public CharBuffer wrapHeaders() {
        StringBuilder sb = new StringBuilder();
        if (status == null) {
            logger.log(Level.WARNING, "no status code set by handlers, assuming OK");
            setResponseStatus(HttpResponseStatus.OK);
        }
        sb.append(version.text()).append(SPACE).append(status.code()).append(SPACE).append(status.reasonPhrase()).append(CRLF);
        for (Pair<String, String> e : headers.entries()) {
            sb.append(e.getKey().toLowerCase(Locale.ROOT)).append(COLON).append(SPACE).append(e.getValue()).append(CRLF);
        }
        sb.append(CRLF);
        return CharBuffer.wrap(sb);
    }
}
