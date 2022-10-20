package org.xbib.net.http.server.netty;

import io.netty.util.AttributeKey;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.HttpServerConfig;

public class NettyHttpServerConfig extends HttpServerConfig {

    public static final AttributeKey<HttpAddress> ATTRIBUTE_KEY_HTTP_ADDRESS = AttributeKey.valueOf("_address");

    /**
     * Enforce the transport class name if many transport providers are given.
     * Default is null so no transport provider is enforced, the first one
     * will be chosen that is supported. The builtin transport class is the {@link NioServerTransportProvider}.
     */
    private String transportProviderName = null;

    /**
     * Let Netty decide about parent thread count.
     */
    private int parentThreadCount = 0;

    /**
     * Let Netty decide about child thread count.
     */
    private int childThreadCount = 0;

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
     * HTTP/1 pipelining. Disabled by default.
     */
    private boolean isPipeliningEnabled = false;

    /**
     * HTTP/1 pipelining capacity. 1024 is very high, it means
     * 1024 requests can be present for a single client.
     */
    private int pipeliningCapacity = 1024;

    /**
     * This is Netty's default.
     */
    private int maxCompositeBufferComponents = 1024;

    /**
     * Default for compression.
     */
    private boolean enableCompression = true;

    /**
     * Default for decompression.
     */
    private boolean enableDecompression = true;

    public NettyHttpServerConfig() {
    }

    public NettyHttpServerConfig setTransportProviderName(String transportProviderName) {
        this.transportProviderName = transportProviderName;
        return this;
    }

    public String getTransportProviderName() {
        return transportProviderName;
    }

    public NettyHttpServerConfig setParentThreadCount(int parentThreadCount) {
        this.parentThreadCount = parentThreadCount;
        return this;
    }

    public int getParentThreadCount() {
        return parentThreadCount;
    }

    public NettyHttpServerConfig setChildThreadCount(int childThreadCount) {
        this.childThreadCount = childThreadCount;
        return this;
    }

    public int getChildThreadCount() {
        return childThreadCount;
    }

    public NettyHttpServerConfig setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
        return this;
    }
    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public NettyHttpServerConfig setMaxHeadersSize(int maxHeadersSize) {
        this.maxHeadersSize = maxHeadersSize;
        return this;
    }

    public int getMaxHeadersSize() {
        return maxHeadersSize;
    }

    public NettyHttpServerConfig setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public NettyHttpServerConfig setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
        return this;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public NettyHttpServerConfig setPipelining(boolean isPipeliningEnabled) {
        this.isPipeliningEnabled = isPipeliningEnabled;
        return this;
    }

    public boolean isPipeliningEnabled() {
        return isPipeliningEnabled;
    }

    public NettyHttpServerConfig setPipeliningCapacity(int pipeliningCapacity) {
        this.pipeliningCapacity = pipeliningCapacity;
        return this;
    }

    public int getPipeliningCapacity() {
        return pipeliningCapacity;
    }

    public NettyHttpServerConfig setMaxCompositeBufferComponents(int maxCompositeBufferComponents) {
        this.maxCompositeBufferComponents = maxCompositeBufferComponents;
        return this;
    }

    public int getMaxCompositeBufferComponents() {
        return maxCompositeBufferComponents;
    }

    public NettyHttpServerConfig setCompression(boolean enabled) {
        this.enableCompression = enabled;
        return this;
    }

    public boolean isCompressionEnabled() {
        return enableCompression;
    }

    public NettyHttpServerConfig setDecompression(boolean enabled) {
        this.enableDecompression = enabled;
        return this;
    }

    public boolean isDecompressionEnabled() {
        return enableDecompression;
    }

}
