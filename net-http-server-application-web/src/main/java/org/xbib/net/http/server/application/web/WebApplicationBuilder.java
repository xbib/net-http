package org.xbib.net.http.server.application.web;

import org.xbib.net.http.server.application.BaseApplicationBuilder;

import org.xbib.settings.Settings;

public class WebApplicationBuilder extends BaseApplicationBuilder {

    protected String profile;

    protected String name;

    protected WebApplicationBuilder() {
        super();
        this.profile = System.getProperty("application.profile");
        this.name = System.getProperty("application.name");
    }

    @Override
    public WebApplicationBuilder setSettings(Settings settings) {
        super.setSettings(settings);
        return this;
    }

    public WebApplicationBuilder setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public WebApplicationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public WebApplication build() {
        WebApplication webApplication = new WebApplication(this);
        setupApplication(webApplication);
        return webApplication;
    }
}
