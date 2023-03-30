package org.xbib.net.http.server.domain;

import java.util.List;
import org.xbib.net.SecurityDomain;
import org.xbib.net.http.server.HttpHandler;

public interface HttpSecurityDomain extends SecurityDomain {

    List<HttpHandler> getHandlers();
}
