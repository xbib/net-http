package org.xbib.net.http.server.executor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public interface ExecutorBuilder {

    ExecutorBuilder setThreadPrefix(String prefix);

    ExecutorBuilder setThreadCount(int threadCount);

    ExecutorBuilder setQueueCount(int threadQueueCount);

    ExecutorBuilder setKeepAliveTime(int keepAliveTime);

    ExecutorBuilder setKeepAliveTimeUnit(TimeUnit keepAliveTimeUnit);

    ExecutorBuilder setExecutor(ThreadPoolExecutor executor);

    Executor build();
}
