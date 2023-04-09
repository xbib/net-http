package org.xbib.net.http.client.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.util.AttributeKey;
import java.util.ArrayList;
import java.util.List;
import org.xbib.net.SocketConfig;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.client.BackOff;

public class NettyHttpClientConfig {

    public static final AttributeKey<HttpDataFactory> ATTRIBUTE_HTTP_DATAFACTORY = AttributeKey.valueOf("http_datafactory");

    /**
     * If frame logging /traffic logging is enabled or not.
     */
    private boolean debug = false;

    /**
     * Default debug log level.
     */
    private LogLevel debugLogLevel = LogLevel.DEBUG;

    protected SocketConfig socketConfig = new SocketConfig();

    private String transportProviderName = null;

    /**
     * If set to 0, then Netty will decide about thread count.
     * Default is Runtime.getRuntime().availableProcessors() * 2
     */
    private int threadCount = 0;

    /**
     * Set HTTP initial line length to 4k.
     * See {@link io.netty.handler.codec.http.HttpClientCodec}.
     */
    private int maxInitialLineLength = 4 * 1024;

    /**
     * Set HTTP maximum headers size to 8k.
     * See  {@link io.netty.handler.codec.http.HttpClientCodec}.
     */
    private int maxHeadersSize = 8 * 1024;

    /**
     * Set HTTP chunk maximum size to 8k.
     * See {@link io.netty.handler.codec.http.HttpClientCodec}.
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
     * Default for gzip codec is false
     */
    private boolean isGzipEnabled = false;

    private ByteBufAllocator byteBufAllocator;

    private HttpProxyHandler httpProxyHandler;

    private Socks4ProxyHandler socks4ProxyHandler;

    private Socks5ProxyHandler socks5ProxyHandler;

    private List<HttpAddress> poolNodes = new ArrayList<>();

    private Pool.PoolKeySelectorType poolKeySelectorType = Pool.PoolKeySelectorType.ROUNDROBIN;

    private Integer poolNodeConnectionLimit;

    private Integer retriesPerPoolNode = 0;

    private HttpVersion poolVersion = HttpVersion.HTTP_1_1;

    private boolean poolSecure = false;

    private Http2Settings http2Settings = Http2Settings.defaultSettings();

    private WriteBufferWaterMark writeBufferWaterMark = WriteBufferWaterMark.DEFAULT;

    private BackOff backOff = BackOff.ZERO_BACKOFF;

    private boolean isChunkWriteEnabled = true;

    private boolean isObjectAggregationEnabled = true;

    private boolean isFileUploadEnabled = true;

    public NettyHttpClientConfig() {
        this.byteBufAllocator = ByteBufAllocator.DEFAULT;
    }

    public void setByteBufAllocator(ByteBufAllocator byteBufAllocator) {
        this.byteBufAllocator = byteBufAllocator;
    }

    public ByteBufAllocator getByteBufAllocator() {
        return byteBufAllocator;
    }

    public NettyHttpClientConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public NettyHttpClientConfig enableDebug() {
        this.debug = true;
        return this;
    }

    public NettyHttpClientConfig disableDebug() {
        this.debug = false;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public NettyHttpClientConfig setDebugLogLevel(LogLevel debugLogLevel) {
        this.debugLogLevel = debugLogLevel;
        return this;
    }

    public LogLevel getDebugLogLevel() {
        return debugLogLevel;
    }

    public NettyHttpClientConfig setTransportProviderName(String transportProviderName) {
        this.transportProviderName = transportProviderName;
        return this;
    }

    public String getTransportProviderName() {
        return transportProviderName;
    }

    public NettyHttpClientConfig setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public NettyHttpClientConfig setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        return this;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public NettyHttpClientConfig setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
        return this;
    }

    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public NettyHttpClientConfig setMaxHeadersSize(int maxHeadersSize) {
        this.maxHeadersSize = maxHeadersSize;
        return this;
    }

    public int getMaxHeadersSize() {
        return maxHeadersSize;
    }

    public NettyHttpClientConfig setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public NettyHttpClientConfig setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
        return this;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public NettyHttpClientConfig setMaxCompositeBufferComponents(int maxCompositeBufferComponents) {
        this.maxCompositeBufferComponents = maxCompositeBufferComponents;
        return this;
    }

    public int getMaxCompositeBufferComponents() {
        return maxCompositeBufferComponents;
    }

    public NettyHttpClientConfig setGzipEnabled(boolean gzipEnabled) {
        this.isGzipEnabled = gzipEnabled;
        return this;
    }

    public boolean isGzipEnabled() {
        return isGzipEnabled;
    }

    public NettyHttpClientConfig setHttp2Settings(Http2Settings http2Settings) {
        this.http2Settings = http2Settings;
        return this;
    }

    public Http2Settings getHttp2Settings() {
        return http2Settings;
    }


    public NettyHttpClientConfig setHttpProxyHandler(HttpProxyHandler httpProxyHandler) {
        this.httpProxyHandler = httpProxyHandler;
        return this;
    }

    public HttpProxyHandler getHttpProxyHandler() {
        return httpProxyHandler;
    }

    public NettyHttpClientConfig setSocks4ProxyHandler(Socks4ProxyHandler socks4ProxyHandler) {
        this.socks4ProxyHandler = socks4ProxyHandler;
        return this;
    }

    public Socks4ProxyHandler getSocks4ProxyHandler() {
        return socks4ProxyHandler;
    }

    public NettyHttpClientConfig setSocks5ProxyHandler(Socks5ProxyHandler socks5ProxyHandler) {
        this.socks5ProxyHandler = socks5ProxyHandler;
        return this;
    }

    public Socks5ProxyHandler getSocks5ProxyHandler() {
        return socks5ProxyHandler;
    }

    public NettyHttpClientConfig setPoolNodes(List<HttpAddress> poolNodes) {
        this.poolNodes = poolNodes;
        return this;
    }

    public List<HttpAddress> getPoolNodes() {
        return poolNodes;
    }

    public NettyHttpClientConfig setPoolKeySelectorType(Pool.PoolKeySelectorType poolKeySelectorType) {
        this.poolKeySelectorType = poolKeySelectorType;
        return this;
    }

    public Pool.PoolKeySelectorType getPoolKeySelectorType() {
        return poolKeySelectorType;
    }

    public NettyHttpClientConfig addPoolNode(HttpAddress poolNodeAddress) {
        this.poolNodes.add(poolNodeAddress);
        return this;
    }

    public NettyHttpClientConfig setPoolNodeConnectionLimit(Integer poolNodeConnectionLimit) {
        this.poolNodeConnectionLimit = poolNodeConnectionLimit;
        return this;
    }

    public Integer getPoolNodeConnectionLimit() {
        return poolNodeConnectionLimit;
    }

    public NettyHttpClientConfig setRetriesPerPoolNode(Integer retriesPerPoolNode) {
        this.retriesPerPoolNode = retriesPerPoolNode;
        return this;
    }

    public Integer getRetriesPerPoolNode() {
        return retriesPerPoolNode;
    }

    public NettyHttpClientConfig setPoolVersion(HttpVersion poolVersion) {
        this.poolVersion = poolVersion;
        return this;
    }

    public HttpVersion getPoolVersion() {
        return  poolVersion;
    }

    public NettyHttpClientConfig setPoolSecure(boolean poolSecure) {
        this.poolSecure = poolSecure;
        return this;
    }

    public boolean isPoolSecure() {
        return poolSecure;
    }

    public NettyHttpClientConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        this.writeBufferWaterMark = writeBufferWaterMark;
        return this;
    }

    public WriteBufferWaterMark getWriteBufferWaterMark() {
        return writeBufferWaterMark;
    }

    public NettyHttpClientConfig setBackOff(BackOff backOff) {
        this.backOff = backOff;
        return this;
    }

    public BackOff getBackOff() {
        return backOff;
    }

    public NettyHttpClientConfig setChunkWriteEnabled(boolean isChunkWriteEnabled) {
        this.isChunkWriteEnabled = isChunkWriteEnabled;
        return this;
    }

    public boolean isChunkWriteEnabled() {
        return isChunkWriteEnabled;
    }

    public NettyHttpClientConfig setObjectAggregationEnabled(boolean isObjectAggregationEnabled) {
        this.isObjectAggregationEnabled = isObjectAggregationEnabled;
        return this;
    }

    public boolean isObjectAggregationEnabled() {
        return isObjectAggregationEnabled;
    }

    public NettyHttpClientConfig setFileUploadEnabled(boolean fileUploadEnabled) {
        isFileUploadEnabled = fileUploadEnabled;
        return this;
    }

    public boolean isFileUploadEnabled() {
        return isFileUploadEnabled;
    }
}
