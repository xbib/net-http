import org.xbib.net.http.server.ApplicationModule;
import org.xbib.net.http.server.application.config.ConfigApplicationModule;

module org.xbib.net.http.server.application.config {
    uses org.xbib.config.ConfigLogger;
    exports org.xbib.net.http.server.application.config;
    requires org.xbib.net;
    requires org.xbib.net.http.server;
    requires org.xbib.config;
    requires java.logging;
    provides ApplicationModule with ConfigApplicationModule;
}
