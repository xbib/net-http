package org.xbib.net.http.server;

import java.io.IOException;
import org.xbib.net.Response;

public interface HttpResponse extends Response {

    void close() throws IOException;
}
