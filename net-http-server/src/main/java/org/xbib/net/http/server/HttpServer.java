package org.xbib.net.http.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.BindException;

public interface HttpServer extends Closeable {

    void bind() throws BindException;

    void loop() throws IOException;

    Application getApplication();
}
