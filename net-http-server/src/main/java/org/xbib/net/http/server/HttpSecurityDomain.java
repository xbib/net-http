package org.xbib.net.http.server;

import java.util.List;
import org.xbib.net.SecurityDomain;

public interface HttpSecurityDomain extends SecurityDomain {

    List<HttpHandler> getHandlers();
}
