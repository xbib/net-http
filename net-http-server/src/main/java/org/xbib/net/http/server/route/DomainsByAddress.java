package org.xbib.net.http.server.route;

import org.xbib.datastructures.common.MultiMap;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.HttpDomain;

public interface DomainsByAddress extends MultiMap<HttpAddress, HttpDomain> {
}
