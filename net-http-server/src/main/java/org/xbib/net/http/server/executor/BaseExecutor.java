package org.xbib.net.http.server.executor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseExecutor implements Executor {

    private static final Logger logger = Logger.getLogger(BaseExecutor.class.getName());

    private final BaseExecutorBuilder builder;

    protected BaseExecutor(BaseExecutorBuilder builder) {
        this.builder = builder;
    }

    public static BaseExecutorBuilder builder() {
        return new BaseExecutorBuilder();
    }

    @Override
    public void execute(Callable<?> callable) {
        builder.executor.submit(callable);
    }

    @Override
    public void shutdown() throws IOException {
        builder.executor.shutdown();
        try {
            if (!builder.executor.awaitTermination(builder.threadKeepAliveTime, builder.threadKeepAliveTimeUnit)) {
                List<Runnable> list = builder.executor.shutdownNow();
                logger.log(Level.WARNING, "unable to stop runnables " + list);
            }
        } catch (InterruptedException e) {
            List<Runnable> list = builder.executor.shutdownNow();
            logger.log(Level.WARNING, "unable to stop runnables " + list);
            throw new IOException(e);
        }
    }
}
