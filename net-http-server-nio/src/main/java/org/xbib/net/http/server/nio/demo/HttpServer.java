package org.xbib.net.http.server.nio.demo;

import java.io.IOException;

public class HttpServer {

    HttpServer() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 8080;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.provider(socketContext -> new HttpServerHandler(socketContext, new UriHandler()))
                .connect(port);
    }
}
