import org.xbib.net.http.client.netty.HttpChannelInitializer;
import org.xbib.net.http.client.netty.secure.ClientSecureSocketProvider;
import org.xbib.net.http.client.netty.secure.JdkClientSecureSocketProvider;
import org.xbib.net.http.client.netty.secure.http1.Https1ChannelInitializer;
import org.xbib.net.http.client.netty.secure.http2.Https2ChannelInitializer;

module org.xbib.net.http.client.netty.secure {
    exports org.xbib.net.http.client.netty.secure;
    exports org.xbib.net.http.client.netty.secure.http1;
    exports org.xbib.net.http.client.netty.secure.http2;
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.client;
    requires org.xbib.net.http.client.netty;
    requires org.xbib.net.security;
    requires io.netty.handler;
    requires io.netty.codec.http;
    requires io.netty.codec.http2;
    requires io.netty.handler.proxy;
    requires io.netty.transport;
    requires java.logging;
    requires io.netty.common;
    uses ClientSecureSocketProvider;
    provides ClientSecureSocketProvider with JdkClientSecureSocketProvider;
    uses HttpChannelInitializer;
    provides HttpChannelInitializer with Https1ChannelInitializer, Https2ChannelInitializer;
}
