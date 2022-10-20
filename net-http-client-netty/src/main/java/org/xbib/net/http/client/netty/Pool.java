package org.xbib.net.http.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.AttributeKey;
import java.io.Closeable;
import java.io.IOException;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpVersion;

public interface Pool extends Closeable {

    AttributeKey<HttpAddress> POOL_ATTRIBUTE_KEY = AttributeKey.valueOf("__pool");

    void init(Bootstrap bootstrap, ChannelPoolHandler channelPoolHandler, int count) throws IOException;

    HttpVersion getVersion();

    Channel acquire() throws Exception;

    void release(Channel channel, boolean close) throws Exception;

    enum PoolKeySelectorType {
        RANDOM, ROUNDROBIN
    }
}
