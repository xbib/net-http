package org.xbib.net.http.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.PathNormalizer;

import java.util.Objects;

public class BaseHttpServiceBuilder implements HttpServiceBuilder {

    protected Collection<HttpMethod> methods;

    protected String pathSpec;

    protected Collection<HttpHandler> handlers;

    protected Collection<ParameterDefinition> parameterDefinitions;

    protected HttpSecurityDomain securityDomain;

    protected BaseHttpServiceBuilder() {
        this.methods = new HashSet<>();
        methods.add(HttpMethod.GET);
        this.pathSpec = "/**";
        this.handlers = null;
        this.securityDomain = null;
    }

    @Override
    public BaseHttpServiceBuilder setMethod(HttpMethod... method) {
        this.methods = new LinkedHashSet<>(Arrays.asList(method));
        return this;
    }

    public BaseHttpServiceBuilder setPath(String path) {
        if (path != null) {
            this.pathSpec = PathNormalizer.normalize(path);
        }
        return this;
    }

    @Override
    public BaseHttpServiceBuilder setHandler(HttpHandler... handler) {
        this.handlers = Arrays.asList(handler);
        return this;
    }

    @Override
    public HttpServiceBuilder setParameterDefinition(ParameterDefinition... parameterDefinition) {
        this.parameterDefinitions = Arrays.asList(parameterDefinition);
        return this;
    }

    @Override
    public BaseHttpServiceBuilder setSecurityDomain(HttpSecurityDomain securityDomain) {
        this.securityDomain = securityDomain;
        return this;
    }

    public BaseHttpService build() {
        Objects.requireNonNull(handlers);
        return new BaseHttpService(this);
    }
}
