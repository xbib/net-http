package org.xbib.net.http.server.executor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.util.NamedThreadFactory;

public class BaseExecutorBuilder implements ExecutorBuilder {

    private static final Logger logger = Logger.getLogger(BaseExecutorBuilder.class.getName());

    protected String threadPrefix;

    protected int threadCount;

    protected int threadQueueCount;

    protected int threadKeepAliveTime;

    protected TimeUnit threadKeepAliveTimeUnit;

    protected ThreadPoolExecutor executor;

    protected BaseExecutorBuilder() {
        this.threadPrefix = "org-xbib-net-server-executor";
        this.threadCount = Runtime.getRuntime().availableProcessors();
        this.threadQueueCount = 0; // use fair synchronous queue
        this.threadKeepAliveTime = 10;
        this.threadKeepAliveTimeUnit = TimeUnit.SECONDS;
    }

    @Override
    public ExecutorBuilder setThreadPrefix(String threadPrefix) {
        this.threadPrefix = threadPrefix;
        return this;
    }

    @Override
    public ExecutorBuilder setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    @Override
    public ExecutorBuilder setQueueCount(int threadQueueCount) {
        this.threadQueueCount = threadQueueCount;
        return this;
    }

    @Override
    public ExecutorBuilder setKeepAliveTime(int keepAliveTime) {
        this.threadKeepAliveTime = keepAliveTime;
        return this;
    }

    @Override
    public ExecutorBuilder setKeepAliveTimeUnit(TimeUnit keepAliveTimeUnit) {
        this.threadKeepAliveTimeUnit = keepAliveTimeUnit;
        return this;
    }

    @Override
    public ExecutorBuilder setExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public Executor build() {
        if (executor == null) {
            this.executor = new BaseThreadPoolExecutor(threadCount, threadQueueCount,
                    threadKeepAliveTime, threadKeepAliveTimeUnit,
                    new NamedThreadFactory(threadPrefix));
            this.executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) ->
                    logger.log(Level.SEVERE, "rejected " + runnable + " for thread pool executor = " + threadPoolExecutor));
        }
        return new BaseExecutor(this);
    }
}
