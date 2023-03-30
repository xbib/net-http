package org.xbib.net.http.server.route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.handler.BadRequestHandler;
import org.xbib.net.http.server.handler.ForbiddenHandler;
import org.xbib.net.http.server.handler.InternalServerErrorHandler;
import org.xbib.net.http.server.handler.NotFoundHandler;
import org.xbib.net.http.server.handler.NotImplementedHandler;
import org.xbib.net.http.server.handler.UnauthorizedHandler;
import org.xbib.net.http.server.handler.VersionNotSupportedHandler;

public class BaseHttpRouterBuilder implements HttpRouterBuilder {

    protected String prefix;

    protected final Collection<HttpDomain> domains;

    protected final Map<Integer, HttpHandler> handlers;

    protected BaseHttpRouterBuilder() {
        prefix = "";
        domains = new ArrayList<>();
        handlers = new HashMap<>();
        handlers.put(400, new BadRequestHandler());
        handlers.put(401, new UnauthorizedHandler());
        handlers.put(403, new ForbiddenHandler());
        handlers.put(404, new NotFoundHandler());
        handlers.put(500, new InternalServerErrorHandler());
        handlers.put(501, new NotImplementedHandler());
        handlers.put(505, new VersionNotSupportedHandler());
    }

    @Override
    public BaseHttpRouterBuilder setPrefix(String prefix) {
        Objects.requireNonNull(prefix);
        // Add ending slash if missing.
        // We require a prefix with ending slash. Otherwise, obscure things can happen in path parameter handling,
        // if a path parameter has a common prefix with this prefix.
        this.prefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
        return this;
    }

    @Override
    public BaseHttpRouterBuilder setHandler(Integer code, HttpHandler httpHandler) {
        handlers.put(code, httpHandler);
        return this;
    }

    @Override
    public BaseHttpRouterBuilder addDomain(HttpDomain domain) {
        this.domains.add(domain);
        return this;
    }

    @Override
    public BaseHttpRouter build() {
        if (domains.isEmpty()) {
            throw new IllegalArgumentException("no domain configured, unable to continue");
        }
        return new BaseHttpRouter(this);
    }
}
