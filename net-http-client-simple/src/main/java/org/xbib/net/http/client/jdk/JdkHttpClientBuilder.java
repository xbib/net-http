package org.xbib.net.http.client.jdk;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JdkHttpClientBuilder {

    private static final Logger logger = Logger.getLogger(JdkHttpClientBuilder.class.getName());

    JdkHttpClientConfig jdkHttpClientConfig;

    JdkHttpClientBuilder() {
    }

    public JdkHttpClientBuilder setConfig(JdkHttpClientConfig JdkHttpClientConfig) {
        this.jdkHttpClientConfig = JdkHttpClientConfig;
        return this;
    }

    public JdkHttpClient build() throws IOException {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "installed security providers = " +
                    Arrays.stream(Security.getProviders()).map(Provider::getName).collect(Collectors.toList()));
        }
        if (jdkHttpClientConfig == null) {
            jdkHttpClientConfig = createEmptyConfig();
        }
        return new JdkHttpClient(this);
    }

    protected JdkHttpClientConfig createEmptyConfig() {
        return new JdkHttpClientConfig();
    }
}
