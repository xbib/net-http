import org.xbib.net.http.server.netty.secure.ServerSecureSocketProvider;
import org.xbib.net.http.client.netty.secure.ClientSecureSocketProvider;
import org.xbib.net.http.netty.boringssl.BoringSSLClientSecureSocketProvider;
import org.xbib.net.http.netty.boringssl.BoringSSLServerSecureSocketProvider;

module org.xbib.net.http.netty.boringssl {
    exports org.xbib.net.http.netty.boringssl;
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.server;
    requires org.xbib.net.http.server.netty.secure;
    requires org.xbib.net.http.client.netty.secure;
    requires io.netty.handler;
    requires io.netty.codec.http2;
    provides ClientSecureSocketProvider with BoringSSLClientSecureSocketProvider;
    provides ServerSecureSocketProvider with BoringSSLServerSecureSocketProvider;
}
