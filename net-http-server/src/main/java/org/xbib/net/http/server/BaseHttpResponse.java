package org.xbib.net.http.server;

public abstract class BaseHttpResponse implements HttpResponse {

    protected final BaseHttpResponseBuilder builder;

    protected BaseHttpResponse(BaseHttpResponseBuilder builder) {
        this.builder = builder;
    }
}
