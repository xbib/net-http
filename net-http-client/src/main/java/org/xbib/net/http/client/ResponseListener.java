package org.xbib.net.http.client;

@FunctionalInterface
public interface ResponseListener<R extends HttpResponse> {

    void onResponse(R response);
}
