package org.xbib.net.http.server.decorate;

import java.util.Collection;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpSecurityDomain;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.HttpService;

import java.io.IOException;
import java.util.Objects;

public class DecoratingHttpService implements HttpService {

    private final HttpService delegate;

    private final HttpHandler handler;

    public DecoratingHttpService(HttpService delegate, HttpHandler handler) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(handler);
        this.delegate = delegate;
        this.handler = handler;
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        handler.handle(context);
        delegate.handle(context);
    }

    @Override
    public Collection<HttpMethod> getMethods() {
        return delegate.getMethods();
    }

    @Override
    public String getPathSpecification() {
        return delegate.getPathSpecification();
    }

    @Override
    public Collection<HttpHandler> getHandlers() {
        return delegate.getHandlers();
    }

    @Override
    public Collection<ParameterDefinition> getParameterDefinitions() {
        return delegate.getParameterDefinitions();
    }

    @Override
    public HttpSecurityDomain getSecurityDomain() {
        return delegate.getSecurityDomain();
    }
}
