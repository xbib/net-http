import org.xbib.net.http.server.ApplicationModule;
import org.xbib.net.http.server.application.database.DatabaseApplicationModule;

module org.xbib.net.http.server.application.database {
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.server;
    requires org.xbib.jdbc.query;
    requires org.xbib.jdbc.connection.pool;
    requires org.xbib.datastructures.tiny;
    requires java.logging;
    requires java.sql;
    provides ApplicationModule with DatabaseApplicationModule;
}
