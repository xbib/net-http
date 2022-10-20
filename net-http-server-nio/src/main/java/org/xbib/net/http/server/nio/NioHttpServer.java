package org.xbib.net.http.server.nio;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NioHttpServer implements HttpServer {

    private static final Logger logger = Logger.getLogger(NioHttpServer.class.getName());

    private final NioHttpServerBuilder builder;

    private final ExecutorService workerPool;

    private final Map<HttpAddress, ServerSocketChannel> serverSockets;

    NioHttpServer(NioHttpServerBuilder builder) {
        this.builder = builder;
        this.workerPool = Executors.newCachedThreadPool();
        this.serverSockets = new HashMap<>();
    }

    public static NioHttpServerBuilder builder() {
        return new NioHttpServerBuilder();
    }

    @Override
    public void bind() throws BindException {
        for (HttpAddress httpAddress : getApplication().getAddresses()) {
            try {
                logger.log(Level.INFO, () -> "trying to bind to " + httpAddress);
                ServerSocketChannel channel = ServerSocketChannel.open();
                if (channel.isOpen()) {
                    channel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
                    channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    channel.bind(httpAddress.getInetSocketAddress());
                }
                serverSockets.put(httpAddress, channel);
            } catch (Exception e) {
                throw new BindException(e.getMessage());
            }
        }
        try {
            Map<SelectionKey, HttpAddress> httpAddressMap = new HashMap<>();
            Selector selector = Selector.open();
            for (Map.Entry<HttpAddress, ServerSocketChannel> entry : serverSockets.entrySet()) {
                final HttpAddress httpAddress = entry.getKey();
                final ServerSocketChannel channel = entry.getValue();
                channel.configureBlocking(false);
                SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
                httpAddressMap.put(key, httpAddress);
            }
            while (true) {
                int num = selector.select();
                if (num == 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    HttpAddress httpAddress = httpAddressMap.get(key);
                    iterator.remove();
                    if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
                    } else if (key.isReadable()) {
                        //workerPool.submit(() -> {
                        try {
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            InputStream inputStream = Channels.newInputStream(socketChannel);
                            OutputStream outputStream = Channels.newOutputStream(socketChannel);
                            HttpResponseBuilder responseBuilder = createResponse(outputStream);
                            HttpRequestBuilder requestBuilder = createRequest(inputStream,
                                    httpAddress,
                                    (InetSocketAddress) socketChannel.getLocalAddress(),
                                    (InetSocketAddress) socketChannel.getRemoteAddress());
                            handle(requestBuilder, responseBuilder);
                            socketChannel.close();
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // });
                    } else if (key.isWritable()) {
                        logger.log(Level.WARNING, "nothing to write");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void loop() throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IOException();
        }
    }

    @Override
    public Application getApplication() {
        return builder.application;
    }

    public void handle(HttpRequestBuilder httpRequestBuilder, HttpResponseBuilder httpResponseBuilder) throws IOException {
        getApplication().dispatch(httpRequestBuilder, httpResponseBuilder);
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<HttpAddress, ServerSocketChannel> entry : serverSockets.entrySet()) {
            entry.getValue().close();
        }
    }

    protected HttpRequestBuilder createRequest(InputStream inputStream,
                                               HttpAddress httpAddress,
                                               InetSocketAddress localAddress,
                                               InetSocketAddress remoteAddress) throws IOException {
        String firstLine = readLine(inputStream);
        HttpVersion httpVersion = extractVersion(firstLine);
        String requestURI = firstLine.split("\\s+", 3)[1];
        HttpMethod httpMethod = extractMethod(firstLine);
        HttpHeaders headers = extractHeaders(inputStream);
        ByteBuffer byteBuffer = extractBody(inputStream, headers);
        return HttpRequest.builder()
                .setBaseURL(httpAddress,
                        requestURI,
                        headers.get(HttpHeaderNames.HOST))
                .setLocalAddress(localAddress)
                .setRemoteAddress(remoteAddress)
                .setMethod(httpMethod)
                .setVersion(httpVersion)
                .setHeaders(headers)
                .setBody(byteBuffer);
    }

    protected HttpResponseBuilder createResponse(OutputStream outputStream) {
        return HttpResponse.builder()
                .setOutputStream(outputStream);
    }

    private static HttpVersion extractVersion(String headerLine) throws IllegalArgumentException {
        Matcher m = Pattern.compile("HTTP/(\\d+)\\.(\\d+)").matcher(headerLine);
        if (m.find()) {
            if ((Integer.parseInt(m.group(1)) == 1) && (Integer.parseInt(m.group(2)) == 1)) {
                return HttpVersion.HTTP_1_1;
            } else if ((Integer.parseInt(m.group(1)) == 1) && (Integer.parseInt(m.group(2)) == 0)) {
                return HttpVersion.HTTP_1_0;
            } else {
                throw new IllegalArgumentException("unknown HTTP version: " + headerLine);
            }
        } else {
            throw new IllegalArgumentException("unknown HTTP version: " + headerLine);
        }
    }

    private static HttpMethod extractMethod(String headerLine) throws IllegalArgumentException {
        String method = headerLine.split("\\s+")[0];
        if (method != null) {
            return HttpMethod.valueOf(method);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static HttpHeaders extractHeaders(InputStream inputStream) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        String nextLine;
        while (!(nextLine = readLine(inputStream)).equals("")) {
            String[] values = nextLine.split(":", 2);
            headers.add(values[0].toLowerCase(Locale.ROOT), values[1].trim());
        }
        return headers;
    }

    private static ByteBuffer extractBody(InputStream inputStream, HttpHeaders headers) throws IOException {
        String contentLength = headers.get(HttpHeaderNames.CONTENT_LENGTH);
        ByteBuffer byteBuffer = null;
        if (contentLength != null) {
            int size = Integer.parseInt(contentLength);
            byte[] data = new byte[size];
            int n = inputStream.read(data, 0, size);
            if (n == size) {
                byteBuffer = ByteBuffer.wrap(data);
            }
        }
        return byteBuffer;
    }

    private static String readLine(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean crRead = false;
        int n;
        while ((n = inputStream.read()) != -1) {
            if (n == '\r') {
                crRead = true;
            } else if (n == '\n' && crRead) {
                return result.toString();
            } else {
                result.append((char) n);
            }
        }
        return result.toString();
    }
}
