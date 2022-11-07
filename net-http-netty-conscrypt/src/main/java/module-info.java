import org.xbib.net.http.server.netty.secure.ServerSecureSocketProvider;
import org.xbib.net.http.client.netty.secure.ClientSecureSocketProvider;
import org.xbib.net.http.netty.conscrypt.ConscryptClientSecureSocketProvider;
import org.xbib.net.http.netty.conscrypt.ConscryptServerSecureSocketProvider;

module org.xbib.net.http.netty.conscrypt {
    exports org.xbib.net.http.netty.conscrypt;
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.client.netty.secure;
    requires org.xbib.net.http.server;
    requires org.xbib.net.http.server.netty.secure;
    requires io.netty.handler;
    provides ClientSecureSocketProvider with ConscryptClientSecureSocketProvider;
    provides ServerSecureSocketProvider with ConscryptServerSecureSocketProvider;
}
