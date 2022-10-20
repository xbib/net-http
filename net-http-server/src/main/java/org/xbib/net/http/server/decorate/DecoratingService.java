package org.xbib.net.http.server.decorate;

import org.xbib.net.http.server.HttpServerConfig;
import org.xbib.net.http.server.Service;

public abstract class DecoratingService extends AbstractUnwrappable<Service> implements Service {

    protected DecoratingService(Service delegate) {
        super(delegate);
    }

    @Override
    public void serviceAdded(HttpServerConfig cfg) throws Exception {
        unwrap().serviceAdded(cfg);
    }
}
