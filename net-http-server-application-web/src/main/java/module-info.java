module org.xbib.net.http.server.application.web {
    uses org.xbib.config.ConfigLogger;
    exports org.xbib.net.http.server.application.web;
    exports org.xbib.net.http.server.application.web.groovy;
    requires org.xbib.net;
    requires org.xbib.net.mime;
    requires org.xbib.net.http;
    requires org.xbib.net.http.server;
    requires org.xbib.net.http.server.netty;
    requires org.xbib.net.http.server.netty.secure;
    requires org.xbib.net.http.template.groovy;
    requires org.xbib.net.http.j2html;
    requires org.xbib.datastructures.tiny;
    requires org.xbib.jdbc.query;
    requires org.xbib.config;
    requires java.logging;
    requires com.j2html;
}
