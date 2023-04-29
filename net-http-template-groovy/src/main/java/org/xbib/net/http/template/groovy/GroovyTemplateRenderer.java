package org.xbib.net.http.template.groovy;

import groovy.lang.Writable;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class GroovyTemplateRenderer implements HttpHandler {

    public GroovyTemplateRenderer() {
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        Writable writable = context.getAttributes().get(Writable.class, "writable");
        if (writable != null) {
            DataBuffer dataBuffer = context.getDataBufferFactory().allocateBuffer();
            try (OutputStream outputStream = dataBuffer.asOutputStream()) {
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                writable.writeTo(writer);
            }
            HttpResponseStatus httpResponseStatus = context.getAttributes().get(HttpResponseStatus.class, "_status", HttpResponseStatus.OK);
            context.status(httpResponseStatus)
                    .header("cache-control", "no-cache") // override default must-revalidate behavior
                    .header("content-length", Integer.toString(dataBuffer.writePosition()))
                    .header(CONTENT_TYPE, "text/html; charset=" + StandardCharsets.UTF_8.displayName())
                    .body(dataBuffer);
        }
    }
}
