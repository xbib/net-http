package org.xbib.net.http.j2html;

import org.xbib.net.http.server.service.BaseHttpService;
import org.xbib.net.http.server.service.BaseHttpServiceBuilder;

public class J2HtmlService extends BaseHttpService {

    protected J2HtmlService(BaseHttpServiceBuilder builder) {
        super(builder);
    }

    public static J2HtmlServiceBuilder builder() {
        return new J2HtmlServiceBuilder();
    }
}
