package org.xbib.net.http.server.application.web.j2html;

import org.xbib.net.Attributes;
import org.xbib.net.Resource;
import org.xbib.net.http.j2html.J2HtmlResourceHandler;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.resource.HtmlTemplateResourceHandler;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.util.ExceptionFormatter;

import java.io.IOException;

import static j2html.TagCreator.body;
import static j2html.TagCreator.code;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.pre;

public class InternalServerErrorHandler extends J2HtmlResourceHandler {

    @Override
    protected Resource createResource(HttpRouterContext httpRouterContext) throws IOException {
        return new InternalServerErrorResource(this, httpRouterContext);
    }

    protected static class InternalServerErrorResource extends J2HtmlResource {

        protected InternalServerErrorResource(HtmlTemplateResourceHandler templateResourceHandler, HttpRouterContext httpRouterContext) throws IOException {
            super(templateResourceHandler, httpRouterContext);
        }

        @Override
        protected String render(Application application, Attributes attributes) {
            String message = attributes.get(String.class, "_message");
            Throwable throwable = attributes.get(Throwable.class, "_throwable");
            String stackTrace = ExceptionFormatter.format(throwable);
            return body(
                    h1("Internal Server Error"),
                    div(message),
                    pre(code(stackTrace))
            ).render();
        }
    }
}
