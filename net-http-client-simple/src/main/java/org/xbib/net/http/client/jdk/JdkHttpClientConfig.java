package org.xbib.net.http.client.jdk;

import org.xbib.net.SocketConfig;
import org.xbib.net.http.client.BackOff;

import java.util.logging.Level;

public class JdkHttpClientConfig {

    /**
     * If frame logging /traffic logging is enabled or not.
     */
    private boolean debug = false;

    /**
     * Default debug log level.
     */
    private Level debugLogLevel = Level.FINE;

    SocketConfig socketConfig = new SocketConfig();

    private String transportProviderName = null;

    /**
     * If set to 0, then Netty will decide about thread count.
     * Default is Runtime.getRuntime().availableProcessors() * 2
     */
    private int threadCount = 0;

    /**
     * Set HTTP initial line length to 4k.
     */
    private int maxInitialLineLength = 4 * 1024;

    /**
     * Set HTTP maximum headers size to 8k.
     */
    private int maxHeadersSize = 8 * 1024;

    /**
     * Set HTTP chunk maximum size to 8k.
     */
    private int maxChunkSize = 8 * 1024;

    /**
     * Set maximum content length to 256 MB.
     */
    private int maxContentLength = 256 * 1024 * 1024;

    /**
     * This is Netty's default.
     */
    private int maxCompositeBufferComponents = 1024;

    /**
     * Default for gzip codec is true
     */
    private boolean gzipEnabled = false;

    private BackOff backOff = BackOff.ZERO_BACKOFF;

    public JdkHttpClientConfig() {
    }

    public JdkHttpClientConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public JdkHttpClientConfig enableDebug() {
        this.debug = true;
        return this;
    }

    public JdkHttpClientConfig disableDebug() {
        this.debug = false;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public JdkHttpClientConfig setDebugLogLevel(Level debugLogLevel) {
        this.debugLogLevel = debugLogLevel;
        return this;
    }

    public Level getDebugLogLevel() {
        return debugLogLevel;
    }

    public JdkHttpClientConfig setTransportProviderName(String transportProviderName) {
        this.transportProviderName = transportProviderName;
        return this;
    }

    public String getTransportProviderName() {
        return transportProviderName;
    }

    public JdkHttpClientConfig setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public JdkHttpClientConfig setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        return this;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public JdkHttpClientConfig setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
        return this;
    }

    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public JdkHttpClientConfig setMaxHeadersSize(int maxHeadersSize) {
        this.maxHeadersSize = maxHeadersSize;
        return this;
    }

    public int getMaxHeadersSize() {
        return maxHeadersSize;
    }

    public JdkHttpClientConfig setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public JdkHttpClientConfig setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
        return this;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public JdkHttpClientConfig setMaxCompositeBufferComponents(int maxCompositeBufferComponents) {
        this.maxCompositeBufferComponents = maxCompositeBufferComponents;
        return this;
    }

    public int getMaxCompositeBufferComponents() {
        return maxCompositeBufferComponents;
    }

    public JdkHttpClientConfig setGzipEnabled(boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
        return this;
    }

    public boolean isGzipEnabled() {
        return gzipEnabled;
    }

    public JdkHttpClientConfig setBackOff(BackOff backOff) {
        this.backOff = backOff;
        return this;
    }

    public BackOff getBackOff() {
        return backOff;
    }

}
