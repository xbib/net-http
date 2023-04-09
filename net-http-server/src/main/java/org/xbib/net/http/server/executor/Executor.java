package org.xbib.net.http.server.executor;

import java.io.IOException;
import java.util.concurrent.Callable;

public interface Executor {

    void execute(Callable<?> callable);

    void shutdown() throws IOException;
}
