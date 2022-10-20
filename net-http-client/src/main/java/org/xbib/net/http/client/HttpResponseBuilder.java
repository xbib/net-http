package org.xbib.net.http.client;

import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.cookie.CookieBox;

public interface HttpResponseBuilder {

    HttpResponseBuilder setHttpAddress(HttpAddress httpAddress);

    HttpResponseBuilder setCookieBox(CookieBox cookieBox);
}
