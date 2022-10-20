import org.xbib.net.http.client.netty.ClientTransportProvider;
import org.xbib.net.http.client.netty.http1.Http1ChannelInitializer;
import org.xbib.net.http.client.netty.http2.Http2ChannelInitializer;
import org.xbib.net.http.client.netty.HttpChannelInitializer;
import org.xbib.net.http.client.netty.NioClientTransportProvider;

module org.xbib.net.http.client.netty {
    exports org.xbib.net.http.client.netty;
    exports org.xbib.net.http.client.netty.http1;
    exports org.xbib.net.http.client.netty.http2;
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.client;
    requires io.netty.buffer;
    requires io.netty.common;
    requires io.netty.transport;
    requires io.netty.handler;
    requires io.netty.codec;
    requires io.netty.codec.http;
    requires io.netty.codec.http2;
    requires io.netty.handler.proxy;
    requires java.logging;
    uses ClientTransportProvider;
    provides ClientTransportProvider with NioClientTransportProvider;
    uses HttpChannelInitializer;
    provides HttpChannelInitializer with Http1ChannelInitializer, Http2ChannelInitializer;
}
