package org.xbib.net.http.netty.client.secure;

import com.sun.management.UnixOperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.xbib.net.http.client.netty.HttpRequest;
import org.xbib.net.http.client.netty.NettyHttpClient;
import org.xbib.net.http.client.netty.NettyHttpClientConfig;
import org.xbib.net.http.client.netty.secure.NettyHttpsClientConfig;

class FileDescriptorLeakTest {

    private static final Logger logger = Logger.getLogger(FileDescriptorLeakTest.class.getName());

    @Test
    void testFileLeak() throws Exception {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        for (int i = 0; i < 3; i++) {
            if (os instanceof UnixOperatingSystemMXBean) {
                logger.info("before: number of open file descriptor : " + ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
            }
            NettyHttpClientConfig config = new NettyHttpsClientConfig()
                    .setDebug(true);
            try (NettyHttpClient client = NettyHttpClient.builder()
                    .setConfig(config)
                    .build()) {
                HttpRequest request = HttpRequest.get()
                        .setURL("https://xbib.org")
                        .setResponseListener(resp -> logger.log(Level.INFO, "got response: " +
                                resp.getHeaders() + resp.getBodyAsChars(StandardCharsets.UTF_8)))
                        .build();
                client.execute(request).get().close();
            }
            if (os instanceof UnixOperatingSystemMXBean){
                logger.info("after: number of open file descriptor : " + ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
            }
        }
    }
}
