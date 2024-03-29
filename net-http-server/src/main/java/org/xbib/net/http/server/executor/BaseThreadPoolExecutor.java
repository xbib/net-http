package org.xbib.net.http.server.executor;

import org.xbib.net.util.ExceptionFormatter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseThreadPoolExecutor extends ThreadPoolExecutor {

    private final Logger logger = Logger.getLogger(BaseThreadPoolExecutor.class.getName());

    public BaseThreadPoolExecutor(int nThreads,
                                  int maxQueue,
                                  long keepAliveTime,
                                  TimeUnit timeUnit,
                                  ThreadFactory threadFactory) {
        super(nThreads, nThreads, keepAliveTime, timeUnit, createBlockingQueue(maxQueue), threadFactory);
        logger.log(Level.FINE, () -> "threadpool executor up with nThreads = " + nThreads +
                " keepAliveTime = " + keepAliveTime +
                " time unit = " + timeUnit +
                " maxQueue = " + maxQueue +
                " threadFactory = " + threadFactory);
    }

    private static BlockingQueue<Runnable> createBlockingQueue(int maxQueue) {
        return maxQueue == 0 ? new SynchronousQueue<>(true) : new ArrayBlockingQueue<>(maxQueue);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new Task<>(callable);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable terminationCause) {
        super.afterExecute(runnable, terminationCause);
        logger.log(Level.FINEST, () -> "after execute of " + runnable);
        if (terminationCause != null) {
            logger.log(Level.SEVERE, terminationCause.getMessage(), terminationCause);
        }
    }
}
