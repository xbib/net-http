package org.xbib.net.http.server.application;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ApplicationTask<T> extends FutureTask<T> {

    private final Callable<T> callable;

    public ApplicationTask(Callable<T> callable) {
        super(callable);
        this.callable = callable;
    }

    public Callable<T> getCallable() {
        return callable;
    }
}
