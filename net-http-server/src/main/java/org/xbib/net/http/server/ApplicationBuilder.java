package org.xbib.net.http.server;

import org.xbib.net.http.server.route.HttpRouter;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Locale;

public interface ApplicationBuilder {

    ApplicationBuilder setThreadCount(int blockingThreadCount);

    ApplicationBuilder setQueueCount(int blockingQueueCount);

    ApplicationBuilder setHome(Path home);

    ApplicationBuilder setContextPath(String contextPath);

    ApplicationBuilder setSecret(String hexSecret);

    ApplicationBuilder setSessionsEnabled(boolean sessionsEnabled);

    ApplicationBuilder setRouter(HttpRouter router);

    ApplicationBuilder setLocale(Locale locale);

    ApplicationBuilder setZoneId(ZoneId zoneId);

    Application build();
}
