import org.xbib.net.http.server.netty.ServerTransportProvider;
import org.xbib.net.http.client.netty.ClientTransportProvider;
import org.xbib.net.http.netty.kqueue.KqueueClientTransportProvider;
import org.xbib.net.http.netty.kqueue.KqueueServerTransportProvider;

module org.xbib.net.http.netty.kqueue {
    exports org.xbib.net.http.netty.kqueue;
    requires org.xbib.net.http.client.netty;
    requires org.xbib.net.http.server.netty;
    requires io.netty.transport;
    requires io.netty.transport.classes.kqueue;
    provides ClientTransportProvider with KqueueClientTransportProvider;
    provides ServerTransportProvider with KqueueServerTransportProvider;
}
