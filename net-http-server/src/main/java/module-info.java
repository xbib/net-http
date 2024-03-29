
module org.xbib.net.http.server {
    uses org.xbib.config.ConfigLogger;
    exports org.xbib.net.http.server;
    exports org.xbib.net.http.server.application;
    exports org.xbib.net.http.server.auth;
    exports org.xbib.net.http.server.cookie;
    exports org.xbib.net.http.server.decorate;
    exports org.xbib.net.http.server.domain;
    exports org.xbib.net.http.server.handler;
    exports org.xbib.net.http.server.ldap;
    exports org.xbib.net.http.server.persist;
    exports org.xbib.net.http.server.persist.file;
    exports org.xbib.net.http.server.persist.memory;
    exports org.xbib.net.http.server.render;
    exports org.xbib.net.http.server.resource;
    exports org.xbib.net.http.server.resource.negotiate;
    exports org.xbib.net.http.server.route;
    exports org.xbib.net.http.server.service;
    exports org.xbib.net.http.server.session;
    exports org.xbib.net.http.server.session.file;
    exports org.xbib.net.http.server.session.memory;
    exports org.xbib.net.http.server.validate;
    exports org.xbib.net.http.server.executor;
    requires org.xbib.net;
    requires org.xbib.net.mime;
    requires org.xbib.net.http;
    requires org.xbib.datastructures.common;
    requires org.xbib.datastructures.tiny;
    requires org.xbib.datastructures.json.tiny;
    requires org.xbib.config;
    requires java.logging;
    requires java.naming;
    requires java.sql;
}
