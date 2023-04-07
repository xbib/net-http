package org.xbib.net.http.server.simple;

import java.util.Collection;
import org.xbib.net.NetworkClass;
import org.xbib.net.NetworkUtils;
import org.xbib.net.SocketConfig;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.server.HttpServer;

import javax.net.ServerSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.executor.CallableReleasable;
import org.xbib.net.http.server.route.HttpRouter;

public class SimpleHttpServer implements HttpServer {

    private static final Logger logger = Logger.getLogger(SimpleHttpServer.class.getName());

    private final SimpleHttpServerBuilder builder;

    private final ExecutorService workerPool;

    private final Map<HttpAddress, ServerSocket> serverSockets;

    protected SimpleHttpServer(SimpleHttpServerBuilder builder) {
        this.builder = builder;
        this.workerPool = Executors.newCachedThreadPool();
        this.serverSockets = new HashMap<>();
    }

    public static SimpleHttpServerBuilder builder() {
        return new SimpleHttpServerBuilder();
    }

    @Override
    public void bind() throws BindException {
        // bind only once per HttpAddress in all domains
        for (HttpAddress httpAddress : builder.application.getAddresses()) {
            logger.log(Level.INFO, () -> "trying to bind to " + httpAddress);
            try {
                InetSocketAddress inetSocketAddress = httpAddress.getInetSocketAddress();
                NetworkClass configuredNetworkClass = builder.httpServerConfig.getNetworkClass();
                NetworkClass detectedNetworkClass = NetworkUtils.getNetworkClass(inetSocketAddress.getAddress());
                if (!NetworkUtils.matchesNetwork(detectedNetworkClass, configuredNetworkClass)) {
                    throw new BindException("unable to bind to " + inetSocketAddress.getAddress() + " because network class " +
                            detectedNetworkClass + " is not allowed by configured network class " + configuredNetworkClass);
                }
                SocketConfig socketConfig = httpAddress.getSocketConfig();
                ServerSocket serverSocket = getServerSocketFactory(httpAddress)
                        .createServerSocket(httpAddress.getPort(), 0, httpAddress.getInetAddress());
                serverSocket.setPerformancePreferences(1, 2, 3);
                serverSocket.setReuseAddress(socketConfig.isReuseAddr());
                if (!serverSocket.isBound()) {
                    serverSocket.bind(inetSocketAddress);
                }
                if (serverSocket.isBound()) {
                    serverSockets.put(httpAddress, serverSocket);
                    logger.log(Level.INFO, () -> "server socket = " + serverSocket +
                            " domains = " + builder.application.getAddresses() + " bound, listening on " + inetSocketAddress);
                } else {
                    logger.log(Level.WARNING, "server socket " + serverSocket + " not bound, something is wrong");
                }
            } catch (IOException e) {
                throw new BindException(e.getMessage());
            }
        }
        if (serverSockets.isEmpty()) {
            return;
        }
        ExecutorService service = Executors.newFixedThreadPool(serverSockets.size());
        for (Map.Entry<HttpAddress, ServerSocket> entry : serverSockets.entrySet()) {
            final HttpAddress httpAddress = entry.getKey();
            final ServerSocket serverSocket = entry.getValue();
            service.submit(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Socket socket = serverSocket.accept();
                        SocketConfig socketConfig = httpAddress.getSocketConfig();
                        socket.setKeepAlive(socketConfig.isKeepAlive());
                        socket.setReuseAddress(socketConfig.isReuseAddr());
                        socket.setTcpNoDelay(socketConfig.isTcpNodelay());
                        workerPool.submit(() -> {
                            try {
                                InputStream inputStream = new BufferedInputStream(socket.getInputStream(), 4096);
                                OutputStream outputStream = socket.getOutputStream();
                                HttpResponseBuilder httpResponseBuilder = createResponse(outputStream);
                                HttpRequestBuilder httpRequestBuilder = createRequest(inputStream,
                                        httpAddress,
                                        (InetSocketAddress) socket.getLocalSocketAddress(),
                                        (InetSocketAddress) socket.getRemoteSocketAddress());
                                dispatch(httpRequestBuilder, httpResponseBuilder);
                            } catch (Throwable t) {
                                logger.log(Level.SEVERE, t.getMessage(), t);
                            } finally {
                                try {
                                    if (!socket.isClosed()) {
                                        socket.close();
                                    }
                                } catch (IOException e) {
                                    logger.log(Level.SEVERE, e.getMessage(), e);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public void dispatch(org.xbib.net.http.server.HttpRequestBuilder requestBuilder,
                         org.xbib.net.http.server.HttpResponseBuilder responseBuilder) {
        CallableReleasable<?> callableReleasable = new CallableReleasable<>() {
            @Override
            public Object call() {
                HttpRouter router = builder.application.getRouter();
                router.route(builder.application, requestBuilder, responseBuilder);
                return true;
            }

            @Override
            public void release() {
                requestBuilder.release();
                responseBuilder.release();
            }
        };
        builder.application.getExecutor().execute(callableReleasable);
    }

    @Override
    public void dispatch(org.xbib.net.http.server.HttpRequestBuilder requestBuilder,
                         org.xbib.net.http.server.HttpResponseBuilder responseBuilder,
                         HttpResponseStatus responseStatus) {
        HttpServerContext httpServerContext = builder.application.createContext(null, requestBuilder, responseBuilder);
        CallableReleasable<?> callableReleasable = new CallableReleasable<>() {
            @Override
            public Object call() {
                HttpRouter router = builder.application.getRouter();
                router.routeStatus(responseStatus, httpServerContext);
                return true;
            }

            @Override
            public void release() {
                requestBuilder.release();
                responseBuilder.release();
            }
        };
        builder.application.getExecutor().execute(callableReleasable);
    }

    @Override
    public Collection<HttpDomain> getDomains() {
        return builder.application.getDomains();
    }

    @Override
    public void loop() throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        logger.log(Level.INFO, "closing");
        workerPool.shutdown();
        for (Map.Entry<HttpAddress, ServerSocket> entry : serverSockets.entrySet()) {
            entry.getValue().close();
            logger.log(Level.INFO, "socket " + entry.getValue() + " closed");
        }
    }

    protected ServerSocketFactory getServerSocketFactory(HttpAddress httpAddress) {
        return ServerSocketFactory.getDefault();
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

    protected static HttpVersion extractVersion(String headerLine) throws IllegalArgumentException {
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

    protected static HttpMethod extractMethod(String headerLine) throws IllegalArgumentException {
        String method = headerLine.split("\\s+")[0];
        if (method != null) {
            return HttpMethod.valueOf(method);
        } else {
            throw new IllegalArgumentException();
        }
    }

    protected static HttpHeaders extractHeaders(InputStream inputStream) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        String nextLine;
        while (!(nextLine = readLine(inputStream)).equals("")) {
            String[] values = nextLine.split(":", 2);
            headers.add(values[0].toLowerCase(Locale.ROOT), values[1].trim());
        }
        return headers;
    }

    protected static ByteBuffer extractBody(InputStream inputStream, HttpHeaders headers) throws IOException {
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

    protected static String readLine(InputStream inputStream) throws IOException {
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
