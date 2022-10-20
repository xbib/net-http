package org.xbib.net.http.server.nio.demo;

public interface SocketHandlerProvider {
    SocketHandler provide(SocketContext socketContext);
}
