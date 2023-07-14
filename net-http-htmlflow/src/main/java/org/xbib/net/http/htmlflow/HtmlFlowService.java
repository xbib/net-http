package org.xbib.net.http.htmlflow;

import org.xbib.net.http.server.service.BaseHttpService;
import org.xbib.net.http.server.service.BaseHttpServiceBuilder;

public class HtmlFlowService extends BaseHttpService {

    protected HtmlFlowService(BaseHttpServiceBuilder builder) {
        super(builder);
    }

    public static HtmlFlowServiceBuilder builder() {
        return new HtmlFlowServiceBuilder();
    }
}
