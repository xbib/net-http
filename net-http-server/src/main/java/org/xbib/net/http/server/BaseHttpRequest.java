package org.xbib.net.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.xbib.datastructures.common.MultiMap;
import org.xbib.datastructures.common.Pair;
import org.xbib.datastructures.json.tiny.Json;
import org.xbib.datastructures.json.tiny.JsonBuilder;
import org.xbib.datastructures.tiny.TinyList;
import org.xbib.datastructures.tiny.TinyMultiMap;
import org.xbib.net.Attributes;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterException;
import org.xbib.net.PercentDecoder;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.auth.BaseAttributes;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.util.ExceptionFormatter;

public abstract class BaseHttpRequest implements HttpRequest {

    private static final Logger logger = Logger.getLogger(BaseHttpRequest.class.getName());

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
            // body may be large, skip it
            //jsonBuilder.buildKey("encoding").buildValue("ISO-8859-1");
            //jsonBuilder.buildKey("body").buildValue(StandardCharsets.ISO_8859_1.decode(builder.getBody()).toString());
            jsonBuilder.endMap();
        } catch (IOException | ParameterException e) {
            // ignore
        }
        return jsonBuilder.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public MultiMap<String, Object> asMultiMap() {
        PercentDecoder percentDecoder = new PercentDecoder();
        MultiMap<String, Object> multiMap = new ParameterMap();
        String contentType = getHeaders().get(HttpHeaderNames.CONTENT_TYPE);
        if (getMethod() == HttpMethod.POST &&
                contentType != null && contentType.contains(HttpHeaderValues.APPLICATION_JSON)) {
            String bodyAsChars = getBodyAsChars(StandardCharsets.UTF_8).toString();
            Map<String, Object> map = Json.toMap(bodyAsChars);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Iterable) {
                    multiMap.putAll(entry.getKey(), (Iterable<Object>) entry.getValue());
                } else {
                    multiMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        try {
            toMultiMapEntry(getParameter().get(Parameter.Domain.PATH),
                    percentDecoder,
                    false,
                    multiMap);
            toMultiMapEntry(getParameter().get(Parameter.Domain.FORM),
                    percentDecoder,
                    HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.equals(contentType),
                    multiMap);
            toMultiMapEntry(getParameter().get(Parameter.Domain.QUERY),
                    percentDecoder,
                    HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.equals(contentType),
                    multiMap);
        } catch (ParameterException e) {
            logger.log(Level.WARNING, e.getMessage(), ExceptionFormatter.format(e));
        }
        return multiMap;
    }

    @SuppressWarnings("unchecked")
    private static void toMultiMapEntry(Parameter parameter,
                                        PercentDecoder percentDecoder,
                                        boolean isFormEncoded,
                                        MultiMap<String, Object> multiMap) {
        for (Pair<String, Object> entry : parameter) {
            try {
                List<Object> list;
                Object value = entry.getValue();
                if (value instanceof List) {
                    list = (List<Object>) value;
                } else if (value != null) {
                    list = List.of(value);
                } else {
                    list = List.of();
                }
                for (Object object : list) {
                    String string = object.toString();
                    if (isFormEncoded) {
                        string = string.replace('+', ' ');
                    }
                    multiMap.put(entry.getKey(), percentDecoder.decode(string));
                }
            } catch (MalformedInputException | UnmappableCharacterException e) {
                logger.log(Level.WARNING, "unable to percent decode parameter: " +
                        entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    private static class ParameterMap extends TinyMultiMap<String, Object> {

        public ParameterMap() {
        }

        @Override
        protected Collection<Object> newValues() {
            // keep values with multiple occurences
            return TinyList.builder();
        }
    }
}
