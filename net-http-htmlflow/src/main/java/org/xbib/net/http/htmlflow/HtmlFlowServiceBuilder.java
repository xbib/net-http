package org.xbib.net.http.htmlflow;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.domain.HttpSecurityDomain;
import org.xbib.net.http.server.service.BaseHttpServiceBuilder;

import java.nio.file.Path;

public class HtmlFlowServiceBuilder extends BaseHttpServiceBuilder {

    protected Path prefix;

    protected String suffix;

    public HtmlFlowServiceBuilder() {
        super();
        this.prefix = null;
    }

    @Override
    public HtmlFlowServiceBuilder setPath(String path) {
        super.setPath(path);
        return this;
    }

    @Override
    public HtmlFlowServiceBuilder setMethod(HttpMethod... httpMethod) {
        super.setMethod(httpMethod);
        return this;
    }

    @Override
    public HtmlFlowServiceBuilder setHandler(HttpHandler... handler) {
        super.setHandler(handler);
        return this;
    }

    @Override
    public HtmlFlowServiceBuilder setParameterDefinition(ParameterDefinition... parameterDefinition) {
        super.setParameterDefinition(parameterDefinition);
        return this;
    }

    @Override
    public HtmlFlowServiceBuilder setSecurityDomain(HttpSecurityDomain securityDomain) {
        super.setSecurityDomain(securityDomain);
        return this;
    }

    public HtmlFlowServiceBuilder setPrefix(Path prefix) {
        this.prefix = prefix;
        return this;
    }

    public HtmlFlowServiceBuilder setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public HtmlFlowService build() {
        if (this.handlers == null) {
            HttpHandler httpHandler = new HtmlFlowResourceHandler(prefix, suffix, "index.java");
            setHandler(httpHandler);
        }
        return new HtmlFlowService(this);
    }
}
