package org.xbib.net.http.template.groovy;

import org.xbib.net.ParameterDefinition;
import org.xbib.net.http.HttpMethod;
import org.xbib.net.http.server.BaseHttpServiceBuilder;

import java.nio.file.Path;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpSecurityDomain;

public class GroovyTemplateServiceBuilder extends BaseHttpServiceBuilder {

    protected Path prefix;

    protected String suffix;

    protected String indexFileName;

    protected String templateName;

    public GroovyTemplateServiceBuilder() {
        super();
        this.prefix = null;
        this.indexFileName = "index.gtpl";
        this.templateName = indexFileName;
    }

    @Override
    public GroovyTemplateServiceBuilder setPath(String path) {
        super.setPath(path);
        return this;
    }

    @Override
    public GroovyTemplateServiceBuilder setMethod(HttpMethod... httpMethod) {
        super.setMethod(httpMethod);
        return this;
    }

    @Override
    public GroovyTemplateServiceBuilder setHandler(HttpHandler... handler) {
        super.setHandler(handler);
        return this;
    }

    @Override
    public GroovyTemplateServiceBuilder setParameterDefinition(ParameterDefinition... parameterDefinition) {
        super.setParameterDefinition(parameterDefinition);
        return this;
    }

    @Override
    public BaseHttpServiceBuilder setSecurityDomain(HttpSecurityDomain securityDomain) {
        super.setSecurityDomain(securityDomain);
        return this;
    }

    public GroovyTemplateServiceBuilder setPrefix(Path prefix) {
        this.prefix = prefix;
        return this;
    }

    public GroovyTemplateServiceBuilder setIndexFileName(String indexFileName) {
        this.indexFileName = indexFileName;
        return this;
    }

    public GroovyTemplateServiceBuilder setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public GroovyTemplateServiceBuilder setTemplateName(String templateName) {
        this.templateName = templateName;
        return this;
    }

    public GroovyTemplateService build() {
        if (this.handlers == null) {
            HttpHandler httpHandler = new GroovyTemplateResourceHandler(prefix, suffix, indexFileName);
            setHandler(httpHandler);
        }
        return new GroovyTemplateService(this);
    }
}
