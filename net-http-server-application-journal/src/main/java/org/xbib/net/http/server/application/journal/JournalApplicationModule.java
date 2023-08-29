package org.xbib.net.http.server.application.journal;

import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.application.BaseApplicationModule;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.service.HttpService;
import org.xbib.settings.Settings;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JournalApplicationModule extends BaseApplicationModule {

    private static final Logger logger = Logger.getLogger(JournalApplicationModule.class.getName());

    private final Journal journal;

    public JournalApplicationModule(Application application, String name, Settings settings) throws IOException {
        super(application, name, settings);
        this.journal = new Journal(settings.get("application.journal", "/var/tmp/application/journal"));
    }

    @Override
    public void onOpen(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest) {
        String stamp = createStamp(httpRequest);
        httpRouterContext.getAttributes().put("_stamp", stamp);
        try {
            journal.logRequest(stamp, httpRequest.asJson());
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void onSuccess(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest) {
        String stamp = httpRouterContext.getAttributes().get(String.class, "_stamp");
        HttpResponseBuilder httpResponseBuilder = httpRouterContext.getAttributes().get(HttpResponseBuilder.class, "response");
        if (stamp != null && httpResponseBuilder != null) {
            try {
                journal.logSuccess(stamp, httpResponseBuilder.getResponseStatus().codeAsText());
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @Override
    public void onFail(HttpRouterContext httpRouterContext, HttpService httpService, HttpRequest httpRequest, Throwable throwable) {
        String stamp = httpRouterContext.getAttributes().get(String.class, "_stamp");
        if (stamp != null) {
            try {
                journal.logFail(stamp, throwable);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    private String createStamp(HttpRequest httpRequest) {
        return httpRequest.getLocalAddress().getAddress().getHostAddress() + "_" +
                httpRequest.getLocalAddress().getPort() + "_" +
                httpRequest.getRemoteAddress().getAddress().getHostAddress() + "_" +
                httpRequest.getRemoteAddress().getPort() + "_" +
                LocalDateTime.now();
    }
}
