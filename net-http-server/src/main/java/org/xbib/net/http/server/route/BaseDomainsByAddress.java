package org.xbib.net.http.server.route;

import org.xbib.datastructures.common.LinkedHashSetMultiMap;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.HttpDomain;

public class BaseDomainsByAddress extends LinkedHashSetMultiMap<HttpAddress, HttpDomain> implements DomainsByAddress {

    public BaseDomainsByAddress() {
    }
}
