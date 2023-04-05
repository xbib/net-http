package org.xbib.net.http.server.application;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.mime.MimeTypeService;

public interface ApplicationBuilder {

    ApplicationBuilder setThreadCount(int blockingThreadCount);

    ApplicationBuilder setQueueCount(int blockingThreadQueueCount);

    ApplicationBuilder setKeepAliveTime(int keepAliveTime);

    ApplicationBuilder setKeepAliveTimeUnit(TimeUnit keepAliveTimeUnit);

    ApplicationBuilder setExecutor(ThreadPoolExecutor executor);

    ApplicationBuilder setHome(Path home);

    ApplicationBuilder setContextPath(String contextPath);

    ApplicationBuilder setSecret(String hexSecret);

    ApplicationBuilder setSessionsEnabled(boolean sessionsEnabled);

    ApplicationBuilder setRouter(HttpRouter router);

    ApplicationBuilder setLocale(Locale locale);

    ApplicationBuilder setZoneId(ZoneId zoneId);

    ApplicationBuilder setMimeTypeService(MimeTypeService mimeTypeService);

    ApplicationBuilder setStaticSuffixes(String... suffixes);

    ApplicationBuilder registerModule(ApplicationModule applicationModule);

    Application build();
}
