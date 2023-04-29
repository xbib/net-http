package org.xbib.net.http.server.application;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.executor.Executor;
import org.xbib.net.http.server.route.HttpRouter;
import org.xbib.net.http.server.session.SessionListener;
import org.xbib.net.mime.MimeTypeService;
import org.xbib.settings.Settings;

public interface Application extends SessionListener, Resolver<Path>, Closeable {

    Collection<HttpDomain> getDomains();

    Set<HttpAddress> getAddresses();

    Locale getLocale();

    ZoneId getZoneId();

    MimeTypeService getMimeService();

    Path getHome();

    String getContextPath();

    Settings getSettings();

    void addModule(ApplicationModule applicationModule);

    Collection<ApplicationModule> getModules();

    HttpRouterContext createContext(HttpDomain domain,
                                    HttpRequestBuilder httpRequestBuilder,
                                    HttpResponseBuilder httpResponseBuilder);

    void onOpen(HttpRouterContext httpRouterContext);

    void onClose(HttpRouterContext httpRouterContext);

    void releaseContext(HttpRouterContext httpRouterContext);

    Executor getExecutor();

    HttpRouter getRouter();

    void close() throws IOException;
}
