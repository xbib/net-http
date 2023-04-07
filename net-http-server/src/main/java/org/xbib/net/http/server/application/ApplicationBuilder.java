package org.xbib.net.http.server.application;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Locale;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.mime.MimeTypeService;

public interface ApplicationBuilder {

    ApplicationBuilder setHome(Path home);

    ApplicationBuilder setContextPath(String contextPath);

    ApplicationBuilder setSecret(String hexSecret);

    ApplicationBuilder setSessionsEnabled(boolean sessionsEnabled);

    ApplicationBuilder setLocale(Locale locale);

    ApplicationBuilder setZoneId(ZoneId zoneId);

    ApplicationBuilder setMimeTypeService(MimeTypeService mimeTypeService);

    ApplicationBuilder setStaticSuffixes(String... suffixes);

    ApplicationBuilder setExecutor(Executor executor);

    ApplicationBuilder setRouter(HttpRouter httpRouter);

    Application build();
}
