package org.xbib.net.http.server.executor;

import java.io.IOException;

public interface Executor {

    /**
     * Execute a task that must be released after execution.
     */
    void execute(CallableReleasable<?> callableReleasable);

    void shutdown() throws IOException;
}
