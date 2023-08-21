package org.xbib.net.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.xbib.datastructures.common.Pair;
import org.xbib.datastructures.json.tiny.JsonBuilder;
import org.xbib.net.Attributes;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterException;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.auth.BaseAttributes;
import org.xbib.net.http.server.route.HttpRouterContext;

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
    public List<Message> getMessages() {
        return builder.messages;
    }

    @Override
    public HttpRouterContext getContext() {
        return builder.httpRouterContext;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public String asJson() {
        JsonBuilder jsonBuilder = JsonBuilder.builder();
        try {
            jsonBuilder.beginMap();
            Map<String, Object> local = Map.of("host", builder.localAddress.getHostString(), "port", builder.localAddress.getPort());
            jsonBuilder.buildKey("local").buildMap(local);
            Map<String, Object> remote = Map.of("host", builder.remoteAddress.getHostString(), "port", builder.remoteAddress.getPort());
            jsonBuilder.buildKey("remote").buildMap(remote);
            jsonBuilder.buildKey("baseurl").buildValue(builder.baseURL.toString());
            jsonBuilder.buildKey("version").buildValue(builder.getVersion().toString());
            jsonBuilder.buildKey("method").buildValue(builder.getMethod().toString());
            Map<String, Object> headerMap = builder.getHeaders().entries().stream()
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (x, y) -> y, LinkedHashMap::new));
            jsonBuilder.buildKey("header").buildMap(headerMap);
            jsonBuilder.buildKey("requesturi").buildValue(builder.getRequestURI());
            jsonBuilder.buildKey("requestpath").buildValue(builder.getRequestPath());
            Parameter queryParameter = builder.parameter.get(Parameter.Domain.QUERY);
            Map<String, Object> queryParameterMap = queryParameter != null ?
                    queryParameter.asMultiMap().asMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new)) : Map.of();
            Parameter pathParameter = builder.parameter.get(Parameter.Domain.PATH);
            Map<String, Object> pathParameterMap = pathParameter != null ?
                    pathParameter.asMultiMap().asMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new)) : Map.of();
            Parameter formParameter = builder.parameter.get(Parameter.Domain.FORM);
            Map<String, Object> formParameterMap = formParameter != null ?
                    formParameter.asMultiMap().asMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new)) : Map.of();
            Parameter cookieParameter = builder.parameter.get(Parameter.Domain.COOKIE);
            Map<String, Object> cookieParameterMap = cookieParameter != null ?
                    cookieParameter.asMultiMap().asMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new)) : Map.of();
            Parameter headerParameter = builder.parameter.get(Parameter.Domain.HEADER);
            Map<String, Object> headerParameterMap = headerParameter != null ?
                    headerParameter.asMultiMap().asMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new)) : Map.of();
            jsonBuilder.buildKey("parameter").buildMap(Map.of("query", queryParameterMap,
                    "path", pathParameterMap,
                    "form", formParameterMap,
                    "cookie", cookieParameterMap,
                    "header", headerParameterMap));
            jsonBuilder.buildKey("sequenceid").buildValue(builder.sequenceId);
            jsonBuilder.buildKey("streamid").buildValue(builder.streamId);
            jsonBuilder.buildKey("requestid").buildValue(builder.requestId);
            // body may be large
            //jsonBuilder.buildKey("encoding").buildValue("ISO-8859-1");
            //jsonBuilder.buildKey("body").buildValue(StandardCharsets.ISO_8859_1.decode(builder.getBody()).toString());
            jsonBuilder.endMap();
        } catch (IOException | ParameterException e) {
            // ignore
        }
        return jsonBuilder.build();
    }
}
