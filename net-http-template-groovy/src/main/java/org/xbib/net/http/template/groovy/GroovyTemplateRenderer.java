package org.xbib.net.http.template.groovy;

import groovy.lang.Writable;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpServerContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class GroovyTemplateRenderer implements HttpHandler {

    public GroovyTemplateRenderer() {
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        Writable writable = context.getAttributes().get(Writable.class, "writable");
        if (writable != null) {
            DataBuffer dataBuffer = context.response().getDataBufferFactory().allocateBuffer();
            try (OutputStream outputStream = dataBuffer.asOutputStream()) {
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                writable.writeTo(writer);
            }
            HttpResponseStatus httpResponseStatus = context.getAttributes().get(HttpResponseStatus.class, "_status", HttpResponseStatus.OK);
            context.response()
                    .setResponseStatus(httpResponseStatus)
                    .setHeader("content-length", Integer.toString(dataBuffer.writePosition()))
                    .setContentType("text/html; charset=" + StandardCharsets.UTF_8.displayName())
                    .setHeader("cache-control", "no-cache")
                    .write(dataBuffer);
        }
    }
}
