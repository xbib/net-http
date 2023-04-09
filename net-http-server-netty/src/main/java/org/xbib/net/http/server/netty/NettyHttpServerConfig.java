package org.xbib.net.http.server.netty;

import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.util.AttributeKey;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.HttpServerConfig;

public class NettyHttpServerConfig extends HttpServerConfig {

    public static final AttributeKey<HttpAddress> ATTRIBUTE_HTTP_ADDRESS = AttributeKey.valueOf("_address");

    public static final AttributeKey<HttpRequestBuilder> ATTRIBUTE_HTTP_REQUEST = AttributeKey.valueOf("_request");

    public static final AttributeKey<HttpResponseBuilder> ATTRIBUTE_HTTP_RESPONSE = AttributeKey.valueOf("response");

    public static final AttributeKey<HttpDataFactory> ATTRIBUTE_HTTP_DATAFACTORY = AttributeKey.valueOf("_datafactory");

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
     * HTTP object aggregation is enabled by default.
     */
    private boolean isObjectAggregationEnabled = true;

    /**
     * This is Netty's default.
     */
    private int maxCompositeBufferComponents = 1024;

    /**
     * Do not write chunks by default.
     */
    private boolean isChunkedWriteEnabled = false;

    /**
     * Compression is enabled by default.
     */
    private boolean isCompressionEnabled = true;

    /**
     * Decompression is enabled by default.
     */
    private boolean isDecompressionEnabled = true;

    /**
     * Disable file upload (POST) by default.
     */
    private boolean isFileUploadEnabled = false;

    private int fileUploadDiskThreshold = 1 *1024 * 1024;

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

    public NettyHttpServerConfig setObjectAggregationEnabled(boolean isObjectAgregationEnabled) {
        this.isObjectAggregationEnabled = isObjectAgregationEnabled;
        return this;
    }

    public boolean isObjectAggregationEnabled() {
        return isObjectAggregationEnabled;
    }

    public NettyHttpServerConfig setMaxCompositeBufferComponents(int maxCompositeBufferComponents) {
        this.maxCompositeBufferComponents = maxCompositeBufferComponents;
        return this;
    }

    public int getMaxCompositeBufferComponents() {
        return maxCompositeBufferComponents;
    }

    public NettyHttpServerConfig setCompression(boolean isCompressionEnabled) {
        this.isCompressionEnabled = isCompressionEnabled;
        return this;
    }

    public boolean isCompressionEnabled() {
        return isCompressionEnabled;
    }

    public NettyHttpServerConfig setDecompression(boolean isDecompressionEnabled) {
        this.isDecompressionEnabled = isDecompressionEnabled;
        return this;
    }

    public boolean isDecompressionEnabled() {
        return isDecompressionEnabled;
    }

    public NettyHttpServerConfig setChunkWriteEnabled(boolean isChunkedWriteEnabled) {
        this.isChunkedWriteEnabled = isChunkedWriteEnabled;
        return this;
    }

    public boolean isChunkedWriteEnabled() {
        return isChunkedWriteEnabled;
    }

    public NettyHttpServerConfig setFileUploadEnabled(boolean isFileUploadEnabled) {
        this.isFileUploadEnabled = isFileUploadEnabled;
        return this;
    }

    public boolean isFileUploadEnabled() {
        return isFileUploadEnabled;
    }

    public NettyHttpServerConfig setFileUploadDiskThreshold(int fileUploadDiskThreshold) {
        this.fileUploadDiskThreshold = fileUploadDiskThreshold;
        return this;
    }

    public int getFileUploadDiskThreshold() {
        return fileUploadDiskThreshold;
    }
}
