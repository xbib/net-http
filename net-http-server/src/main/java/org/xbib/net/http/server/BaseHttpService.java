package org.xbib.net.http.server;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;

import java.io.IOException;
import java.util.Objects;
import org.xbib.net.http.HttpResponseStatus;

public class BaseHttpService implements HttpService {

    private static final Logger logger = Logger.getLogger(BaseHttpService.class.getName());

    private final BaseHttpServiceBuilder builder;

    protected BaseHttpService(BaseHttpServiceBuilder builder) {
        this.builder = builder;
    }

    public static BaseHttpServiceBuilder builder() {
        return new BaseHttpServiceBuilder();
    }

    @Override
    public String getPathSpecification() {
        return builder.pathSpec;
    }

    @Override
    public Collection<HttpMethod> getMethods() {
        return builder.methods;
    }

    @Override
    public Collection<HttpHandler> getHandlers() {
        return builder.handlers;
    }

    @Override
    public Collection<ParameterDefinition> getParameterDefinitions() {
        return builder.parameterDefinitions;
    }

    @Override
    public HttpSecurityDomain getSecurityDomain() {
        return builder.securityDomain;
    }

    @Override
    public void handle(HttpServerContext context) throws IOException {
        if (builder.handlers != null) {
            for (HttpHandler handler : builder.handlers) {
                handler.handle(context);
            }
        } else {
            throw new HttpException("no handler found", context, HttpResponseStatus.NOT_IMPLEMENTED);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseHttpService endpoint = (BaseHttpService) o;
        return Objects.equals(builder.methods, endpoint.builder.methods) &&
                Objects.equals(builder.pathSpec, endpoint.builder.pathSpec) &&
                Objects.equals(builder.handlers, endpoint.builder.handlers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(builder.methods, builder.pathSpec, builder.handlers);
    }

    @Override
    public String toString() {
        return "BaseHttpService[methods=" + builder.methods + ",path=" + builder.pathSpec + ",handler=" + builder.handlers + "]";
    }

}
