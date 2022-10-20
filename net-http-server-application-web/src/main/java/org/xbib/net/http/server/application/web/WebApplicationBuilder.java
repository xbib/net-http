package org.xbib.net.http.server.application.web;

import org.xbib.net.http.server.BaseApplicationBuilder;
import org.xbib.net.http.server.Application;

import org.xbib.settings.Settings;

public class WebApplicationBuilder extends BaseApplicationBuilder {

    protected String profile;

    protected String name;

    protected Settings settings;

    protected WebApplicationBuilder() throws Exception {
        super();
        this.profile = System.getProperty("application.profile");
        this.name = System.getProperty("application.name");
    }

    public WebApplicationBuilder setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public WebApplicationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public WebApplicationBuilder setSettings(Settings settings) {
        this.settings = settings;
        return this;
    }

    @Override
    public Application build() {
        WebApplication webApplication = new WebApplication(this);
        setupApplication(webApplication);
        return webApplication;
    }
}
