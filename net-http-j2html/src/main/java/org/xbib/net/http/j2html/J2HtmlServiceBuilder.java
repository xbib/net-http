package org.xbib.net.http.j2html;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.domain.HttpSecurityDomain;
import org.xbib.net.http.server.service.BaseHttpServiceBuilder;

import java.nio.file.Path;

public class J2HtmlServiceBuilder extends BaseHttpServiceBuilder {

    protected Path prefix;

    protected String suffix;

    public J2HtmlServiceBuilder() {
        super();
        this.prefix = null;
    }

    @Override
    public J2HtmlServiceBuilder setPath(String path) {
        super.setPath(path);
        return this;
    }

    @Override
    public J2HtmlServiceBuilder setMethod(HttpMethod... httpMethod) {
        super.setMethod(httpMethod);
        return this;
    }

    @Override
    public J2HtmlServiceBuilder setHandler(HttpHandler... handler) {
        super.setHandler(handler);
        return this;
    }

    @Override
    public J2HtmlServiceBuilder setParameterDefinition(ParameterDefinition... parameterDefinition) {
        super.setParameterDefinition(parameterDefinition);
        return this;
    }

    @Override
    public J2HtmlServiceBuilder setSecurityDomain(HttpSecurityDomain securityDomain) {
        super.setSecurityDomain(securityDomain);
        return this;
    }

    public J2HtmlServiceBuilder setPrefix(Path prefix) {
        this.prefix = prefix;
        return this;
    }

    public J2HtmlServiceBuilder setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public J2HtmlService build() {
        if (this.handlers == null) {
            HttpHandler httpHandler = new J2HtmlResourceHandler(prefix, suffix, "index.java");
            setHandler(httpHandler);
        }
        return new J2HtmlService(this);
    }
}
