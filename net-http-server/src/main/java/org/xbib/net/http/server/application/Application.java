package org.xbib.net.http.server.application;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.domain.HttpDomain;
import org.xbib.net.http.server.session.SessionListener;
import org.xbib.settings.Settings;

public interface Application extends SessionListener, Resolver<Path>, Closeable {

    Collection<HttpDomain> getDomains();

    Set<HttpAddress> getAddresses();

    Locale getLocale();

    ZoneId getZoneId();

    Path getHome();

    String getContextPath();

    Settings getSettings();

    Collection<ApplicationModule> getModules();

    /**
     * Dispatch a regular request.
     * @param requestBuilder the request
     * @param responseBuilder the response
     */
    void dispatch(HttpRequestBuilder requestBuilder,
                  HttpResponseBuilder responseBuilder);

    void dispatch(HttpRequestBuilder requestBuilder,
                  HttpResponseBuilder responseBuilder,
                  HttpResponseStatus httpResponseStatus);

    HttpServerContext createContext(HttpDomain domain,
                                    HttpRequestBuilder httpRequestBuilder,
                                    HttpResponseBuilder httpResponseBuilder);

    void onOpen(HttpServerContext httpServerContext);

    void onClose(HttpServerContext httpServerContext);

    void close() throws IOException;
}
