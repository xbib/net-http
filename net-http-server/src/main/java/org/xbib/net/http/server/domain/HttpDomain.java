package org.xbib.net.http.server.domain;

import java.util.Collection;
import java.util.Set;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.service.HttpService;

/**
 * The {@code HttpDomain} interface represents a set of domain names attached to an HTTP address.
 */
public interface HttpDomain extends Comparable<HttpDomain> {

    Set<String> getNames();

    HttpAddress getAddress();

    Collection<HttpService> getServices();
}
