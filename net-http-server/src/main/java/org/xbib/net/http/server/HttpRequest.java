package org.xbib.net.http.server;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import org.xbib.datastructures.common.MultiMap;
import org.xbib.net.Attributes;
import org.xbib.net.Parameter;
import org.xbib.net.Request;
import org.xbib.net.URL;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.route.HttpRouterContext;

public interface HttpRequest extends Request {

    URL getServerURL();

    HttpRouterContext getContext();

    String getRequestURI();

    HttpVersion getVersion();

    HttpMethod getMethod();

    HttpHeaders getHeaders();

    Parameter getParameter();

    String getRequestPath();

    Integer getSequenceId();

    Integer getStreamId();

    Long getRequestId();

    ByteBuffer getBody();

    InputStream getInputStream();

    List<Message> getMessages();

    Attributes getAttributes();

    String asJson();

    MultiMap<String, Object> asMultiMap();
}
