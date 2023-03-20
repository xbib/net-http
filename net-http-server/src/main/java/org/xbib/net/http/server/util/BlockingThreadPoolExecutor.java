package org.xbib.net.http.server.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockingThreadPoolExecutor extends ThreadPoolExecutor {

    private final Logger logger = Logger.getLogger(BlockingThreadPoolExecutor.class.getName());

    public BlockingThreadPoolExecutor(int nThreads, int maxQueue,
                                      ThreadFactory threadFactory) {
        this(nThreads, maxQueue, 60L, TimeUnit.SECONDS, threadFactory);
    }

    public BlockingThreadPoolExecutor(int nThreads, int maxQueue,
                                      long keepAliveTime, TimeUnit timeUnit,
                                      ThreadFactory threadFactory) {
        super(nThreads, nThreads, keepAliveTime, timeUnit, createBlockingQueue(maxQueue), threadFactory);
        logger.log(Level.INFO, "blocking threadpool executor up with nThreads = " + nThreads +
                " keepAliveTime = " + keepAliveTime +
                " time unit = " + timeUnit +
                " maxQueue = " + maxQueue +
                " thread factory = " + threadFactory);
    }

    private static BlockingQueue<Runnable> createBlockingQueue(int max) {
        return max == Integer.MAX_VALUE ? new SynchronousQueue<>(true) : new ArrayBlockingQueue<>(max);
    }

    /*
     * Examine Throwable or Error of a thread after execution just to log them.
     */
    @Override
    protected void afterExecute(Runnable runnable, Throwable terminationCause) {
        super.afterExecute(runnable, terminationCause);
        logger.log(Level.FINE, "after dispatching " + runnable + " terminationCause = " + terminationCause);
        Throwable throwable = terminationCause;
        if (throwable == null && runnable instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) runnable;
                if (!future.isDone() && !future.isCancelled()) {
                    logger.log(Level.FINE, "waiting for " + future);
                    future.get();
                }
            } catch (CancellationException ce) {
                logger.log(Level.FINE, ce.getMessage(), ce);
                throwable = ce;
            } catch (ExecutionException ee) {
                logger.log(Level.FINE, ee.getMessage(), ee);
                throwable = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.log(Level.FINE, ie.getMessage(), ie);
            }
        }
        if (throwable != null) {
            logger.log(Level.SEVERE, throwable.getMessage(), throwable);
        }
    }
}
