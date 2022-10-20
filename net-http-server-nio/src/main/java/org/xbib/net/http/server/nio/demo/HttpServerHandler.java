package org.xbib.net.http.server.nio.demo;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpServerHandler implements SocketHandler {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    private final HttpRequestParser httpRequestParser = new HttpRequestParser();

    private final SocketContext socketContext;

    private final HttpRequestHandler handler;

    public HttpServerHandler(SocketContext socketContext, HttpRequestHandler handler) {
        this.socketContext = socketContext;
        this.handler = handler;
    }

    @Override
    public void onRead() throws IOException {
        int i = socketContext.getSocketChannel().read(buffer);
        if (i == -1) {
            socketContext.getSocketChannel().close();
            return;
        }
        buffer.flip();
        httpRequestParser.read(buffer);
        buffer.clear();
        if (!httpRequestParser.isReady()) {
            return;
        }
        HttpRequest request = httpRequestParser.getHttpRequest();
        handler.handle(new HttpContext(request, new HttpResponse(), socketContext));
    }
}
