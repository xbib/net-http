package org.xbib.net.http.server;

import java.net.InetSocketAddress;
import java.util.List;
import org.xbib.net.Attributes;
import org.xbib.net.Parameter;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.auth.BaseAttributes;

public abstract class BaseHttpRequest implements HttpRequest {

    protected final BaseHttpRequestBuilder builder;

    private final Attributes attributes;

    protected BaseHttpRequest(BaseHttpRequestBuilder builder) {
        this.builder = builder;
        this.attributes = new BaseAttributes();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return builder.localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return builder.remoteAddress;
    }

    @Override
    public URL getBaseURL() {
        return builder.baseURL;
    }

    @Override
    public URL getServerURL() {
        return builder.serverURL;
    }

    @Override
    public HttpVersion getVersion() {
        return builder.getVersion();
    }

    @Override
    public HttpMethod getMethod() {
        return builder.getMethod();
    }

    @Override
    public HttpHeaders getHeaders() {
        return builder.getHeaders();
    }

    @Override
    public String getRequestURI() {
        return builder.getRequestURI();
    }

    @Override
    public String getRequestPath() {
        return builder.requestPath;
    }

    @Override
    public Parameter getParameter() {
        return builder.parameter;
    }

    @Override
    public Integer getSequenceId() {
        return builder.sequenceId;
    }

    @Override
    public Integer getStreamId() {
        return builder.streamId;
    }

    @Override
    public Long getRequestId() {
        return builder.requestId;
    }

    @Override
    public List<Part> getParts() {
        return builder.parts;
    }

    @Override
    public HttpServerContext getContext() {
        return builder.httpServerContext;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }
}
