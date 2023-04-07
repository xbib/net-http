package org.xbib.net.http.server.application.web;

import org.xbib.net.http.server.application.BaseApplicationBuilder;

import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.settings.Settings;

public class WebApplicationBuilder extends BaseApplicationBuilder {

    protected String profile;

    protected String name;

    protected WebApplicationBuilder() {
        super();
        this.name = System.getProperty("application.name");
        this.profile = System.getProperty("application.profile");
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
    public WebApplicationBuilder setSettings(Settings settings) {
        this.settings = settings;
        return this;
    }

    @Override
    public WebApplicationBuilder setSecret(String secret) {
        super.setSecret(secret);
        return this;
    }

    @Override
    public WebApplicationBuilder setExecutor(Executor executor) {
        super.setExecutor(executor);
        return this;
    }

    @Override
    public WebApplicationBuilder setRouter(HttpRouter router) {
        super.setRouter(router);
        return this;
    }

    @Override
    public WebApplication build() {
        return new WebApplication(this);
    }
}
