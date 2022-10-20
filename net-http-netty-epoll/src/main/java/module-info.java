import org.xbib.net.http.server.netty.ServerTransportProvider;
import org.xbib.net.http.client.netty.ClientTransportProvider;
import org.xbib.net.http.netty.epoll.EpollClientTransportProvider;
import org.xbib.net.http.netty.epoll.EpollServerTransportProvider;

module org.xbib.net.http.netty.epoll {
    exports org.xbib.net.http.netty.epoll;
    requires org.xbib.net.http.client.netty;
    requires org.xbib.net.http.server.netty;
    requires io.netty.transport;
    requires io.netty.transport.classes.epoll;
    provides ClientTransportProvider with EpollClientTransportProvider;
    provides ServerTransportProvider with EpollServerTransportProvider;
}
