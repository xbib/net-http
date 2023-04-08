package org.xbib.net.http.server.netty.http1;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.netty.HttpRequestBuilder;
import org.xbib.net.http.server.netty.HttpResponse;
import org.xbib.net.http.server.netty.HttpResponseBuilder;
import org.xbib.net.http.server.netty.NettyHttpServer;
import org.xbib.net.http.server.netty.NettyHttpServerConfig;

public class HttpFileUploadHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger logger = Logger.getLogger(HttpFileUploadHandler.class.getName());

    private final NettyHttpServer nettyHttpServer;

    public HttpFileUploadHandler(NettyHttpServer nettyHttpServer) {
        this.nettyHttpServer = nettyHttpServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject httpObject) {
        HttpRequest httpRequest = null;
        HttpPostRequestDecoder httpDecoder = null;
        if (httpObject instanceof HttpRequest) {
            httpRequest = (HttpRequest) httpObject;
            // peek into request if we have a POST request
            if (httpRequest.method() == HttpMethod.POST) {
                HttpDataFactory factory = new DefaultHttpDataFactory(nettyHttpServer.getNettyHttpServerConfig().getFileUploadDiskThreshold());
                httpDecoder = new HttpPostRequestDecoder(factory, httpRequest);
            }
        }
        if (httpDecoder != null) {
            if (httpObject instanceof HttpContent chunk) {
                httpDecoder.offer(chunk);
                try {
                    while (httpDecoder.hasNext()) {
                        InterfaceHttpData data = httpDecoder.next();
                        if (data != null) {
                            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                                logger.log(Level.FINEST, "got file upload");
                                FileUpload fileUpload = (FileUpload) data;
                                requestReceived(ctx, httpRequest, fileUpload);
                            } else {
                                logger.log(Level.FINEST, "got HTTP data type = " + data.getHttpDataType());
                            }
                        }
                    }
                } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
                    logger.log(Level.FINEST, "end of data decoder exception");
                }
                if (chunk instanceof LastHttpContent) {
                    logger.log(Level.FINEST, "destroying HTTP decode");
                    httpDecoder.destroy();
                }
            } else {
                logger.log(Level.FINEST, "not a HttpContent: " );
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.log(Level.SEVERE, cause.getMessage(), cause);
        ctx.close();
    }

    protected void requestReceived(ChannelHandlerContext ctx,
                                   HttpRequest httpRequest,
                                   FileUpload fileUpload) {
        HttpAddress httpAddress = ctx.channel().attr(NettyHttpServerConfig.ATTRIBUTE_KEY_HTTP_ADDRESS).get();
        try {
            HttpResponseBuilder serverResponseBuilder = HttpResponse.builder()
                    .setChannelHandlerContext(ctx);
            serverResponseBuilder.shouldClose("close".equalsIgnoreCase(httpRequest.headers().get(HttpHeaderNames.CONNECTION)));
            // the base URL construction may fail with exception. In that case, we return a built-in 400 Bad Request.
            HttpRequestBuilder httpRequestBuilder = org.xbib.net.http.server.netty.HttpRequest.builder()
                    .setHttpRequest(httpRequest)
                    .addFileUpload(fileUpload)
                    .setBaseURL(httpAddress,
                            httpRequest.uri(),
                            httpRequest.headers().get(HttpHeaderNames.HOST))
                    .setLocalAddress((InetSocketAddress) ctx.channel().localAddress())
                    .setRemoteAddress((InetSocketAddress) ctx.channel().remoteAddress());
            nettyHttpServer.dispatch(httpRequestBuilder, serverResponseBuilder);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "bad request: " + e.getMessage(), e);
            DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_REQUEST);
            ctx.writeAndFlush(fullHttpResponse);
            ctx.close();
        }
    }
}
