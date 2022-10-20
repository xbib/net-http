package org.xbib.net.http.server.validate;

import org.xbib.datastructures.common.Pair;
import org.xbib.net.http.server.UnknownExpectException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.HttpHeaderNames;

public class HttpRequestValidator implements HttpHandler {

    public HttpRequestValidator() {
    }

    @Override
    public void handle(HttpServerContext context) throws HttpException {
        boolean unknownExpect = false;
        for (Pair<String, String> entry : context.request().getHeaders().entries()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name.equalsIgnoreCase(HttpHeaderNames.EXPECT) && !"100-continue".equalsIgnoreCase(value)) {
                unknownExpect = true;
            }
        }
        if (unknownExpect) {
            // RFC2616#14.20: if unknown expect, send 417
            throw new UnknownExpectException("unknown expect", context);
        }
    }
}
