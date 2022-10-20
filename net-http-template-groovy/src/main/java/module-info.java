import org.xbib.net.http.server.ApplicationModule;
import org.xbib.net.http.template.groovy.GroovyTemplateApplicationModule;

module org.xbib.net.http.template.groovy {
    exports org.xbib.net.http.template.groovy;
    requires org.xbib.net;
    requires org.xbib.net.http;
    requires org.xbib.net.http.server;
    requires org.apache.groovy;
    requires org.apache.groovy.templates;
    requires java.logging;
    provides ApplicationModule with GroovyTemplateApplicationModule;
}
