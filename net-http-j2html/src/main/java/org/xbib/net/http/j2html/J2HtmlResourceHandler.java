package org.xbib.net.http.j2html;

import org.xbib.net.Resource;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.resource.HtmlTemplateResource;
import org.xbib.net.http.server.resource.HtmlTemplateResourceHandler;
import org.xbib.net.http.server.route.HttpRouterContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static org.xbib.net.http.HttpHeaderNames.CONTENT_TYPE;

public class J2HtmlResourceHandler extends HtmlTemplateResourceHandler {

    public J2HtmlResourceHandler() {
        this(null, "java", "index.java");
    }

    public J2HtmlResourceHandler(Path root, String suffix, String indexFileName) {
        super(root, suffix, indexFileName);
    }

    @Override
    protected Resource createResource(HttpRouterContext httpRouterContext) throws IOException {
        return new J2HtmlResource(this, httpRouterContext);
    }

    protected static class J2HtmlResource extends HtmlTemplateResource {

        protected HttpRequest request;

        protected J2HtmlResource(HtmlTemplateResourceHandler templateResourceHandler,
                                 HttpRouterContext httpRouterContext) throws IOException {
            super(templateResourceHandler, httpRouterContext);
        }

        @Override
        public void render(HttpRouterContext context) throws IOException {
            this.request = context.getRequest();
            Objects.requireNonNull(responseBuilder);
            DataBuffer dataBuffer = context.getDataBufferFactory().allocateBuffer();
            dataBuffer.write(renderBody(context), StandardCharsets.UTF_8);
            HttpResponseStatus httpResponseStatus = responseBuilder.getResponseStatus();
            if (httpResponseStatus == null) {
                httpResponseStatus = HttpResponseStatus.OK;
            }
            //context.getAttributes().get(HttpResponseStatus.class, "_status", HttpResponseStatus.OK);
            context.status(httpResponseStatus)
                    .header("cache-control", "no-cache") // override default must-revalidate behavior
                    .header("content-length", Integer.toString(dataBuffer.writePosition()))
                    .header(CONTENT_TYPE, "text/html; charset=" + StandardCharsets.UTF_8.displayName())
                    .body(dataBuffer);
        }

        protected String renderBody(HttpRouterContext context) {
            return body(
                    h1("Hello World")
            ).render();
        }
    }
}
