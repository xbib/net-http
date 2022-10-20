package org.xbib.net.http.template.groovy;

import org.xbib.net.http.server.BaseHttpService;

public class GroovyTemplateService extends BaseHttpService {

    private final GroovyTemplateServiceBuilder builder;

    protected GroovyTemplateService(GroovyTemplateServiceBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static GroovyTemplateServiceBuilder builder() {
        return new GroovyTemplateServiceBuilder();
    }

    public String getTemplateName() {
        return builder.templateName;
    }
}
