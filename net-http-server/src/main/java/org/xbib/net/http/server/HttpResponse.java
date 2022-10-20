package org.xbib.net.http.server;

import org.xbib.net.Response;

import java.io.IOException;

public interface HttpResponse extends Response {

    void close() throws IOException;
}
