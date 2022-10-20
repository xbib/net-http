import org.xbib.net.http.server.netty.secure.JdkServerSecureSocketProvider;
import org.xbib.net.http.server.netty.secure.ServerSecureSocketProvider;
import org.xbib.net.security.CertificateProvider;

module org.xbib.net.http.server.netty.secure {
    exports org.xbib.net.http.server.netty.secure.http1;
    exports org.xbib.net.http.server.netty.secure.http2;
    exports org.xbib.net.http.server.netty.secure;
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.server;
    requires org.xbib.net.http.server.netty;
    requires org.xbib.net.security;
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec.http2;
    requires io.netty.common;
    requires io.netty.handler;
    requires io.netty.transport;
    requires java.logging;
    uses CertificateProvider;
    uses ServerSecureSocketProvider;
    provides ServerSecureSocketProvider with JdkServerSecureSocketProvider;
}
