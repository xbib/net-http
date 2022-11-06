import org.xbib.net.buffer.DataBufferFactory;
import org.xbib.net.http.server.netty.HttpChannelInitializer;
import org.xbib.net.http.server.netty.buffer.NettyDataBufferFactory;
import org.xbib.net.http.server.netty.http1.Http1ChannelInitializer;
import org.xbib.net.http.server.netty.http2.Http2ChannelInitializer;
import org.xbib.net.http.server.netty.NioServerTransportProvider;
import org.xbib.net.http.server.netty.ServerTransportProvider;

module org.xbib.net.http.server.netty {
    exports org.xbib.net.http.server.netty;
    exports org.xbib.net.http.server.netty.buffer;
    exports org.xbib.net.http.server.netty.http1;
    exports org.xbib.net.http.server.netty.http2;
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.server;
    requires io.netty.buffer;
    requires io.netty.common;
    requires io.netty.transport;
    requires io.netty.handler;
    requires io.netty.codec;
    requires io.netty.codec.http;
    requires io.netty.codec.http2;
    requires java.logging;
    uses HttpChannelInitializer;
    provides HttpChannelInitializer with Http1ChannelInitializer, Http2ChannelInitializer;
    uses ServerTransportProvider;
    provides ServerTransportProvider with NioServerTransportProvider;
    provides DataBufferFactory with NettyDataBufferFactory;
}
