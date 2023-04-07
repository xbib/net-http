package org.xbib.net.http.server.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class Task<T> extends FutureTask<T> {

    private final Callable<T> callable;

    public Task(Callable<T> callable) {
        super(callable);
        this.callable = callable;
    }

    public Callable<T> getCallable() {
        return callable;
    }
}
