package org.xbib.net.http.netty.client.secure;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThreadLeakTest {

    private static final Logger logger = Logger.getLogger(ThreadLeakTest.class.getName());

    @Test
    void testForLeaks() throws IOException {
        NettyHttpClientConfig config = new NettyHttpsClientConfig();
        try (NettyHttpClient client = NettyHttpClient.builder()
                .setConfig(config)
                .build()) {
        }
    }

    @BeforeAll
    @AfterAll
    void checkThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        logger.log(Level.INFO, "threads = " + threadSet.size() );
        threadSet.forEach( thread -> logger.log(Level.INFO, thread.toString()));
    }
}
