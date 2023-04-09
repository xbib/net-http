package org.xbib.net.http.server;

import org.xbib.net.Response;

public interface HttpResponse extends Response {

    void release();
}
