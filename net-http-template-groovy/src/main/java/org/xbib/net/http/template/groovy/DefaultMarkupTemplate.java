package org.xbib.net.http.template.groovy;

import groovy.text.markup.BaseTemplate;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.URL;
import org.xbib.net.URLBuilder;
import org.xbib.net.buffer.DefaultDataBufferFactory;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.session.Session;
import org.xbib.net.template.URITemplate;
import org.xbib.net.template.vars.Variables;
import org.xbib.net.util.RandomUtil;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

public abstract class DefaultMarkupTemplate extends BaseTemplate {

    private static final Logger logger = Logger.getLogger(DefaultMarkupTemplate.class.getName());

    private final Application application;

    private final HttpRequest request;

    private final HttpResponseBuilder responseBuilder;

    private final Session session;

    public DefaultMarkupTemplate(MarkupTemplateEngine templateEngine,
                                 Map<String,?> model,
                                 Map<String, String> modelTypes,
                                 TemplateConfiguration configuration) {
        super(templateEngine, model, modelTypes, configuration);
        this.application = (Application) model.get("application");
        this.request = (HttpRequest) model.get("request");
        this.responseBuilder = (HttpResponseBuilder) model.get("responsebuilder");
        this.session = (Session) model.get("session");
    }

    public void responseStatus(HttpResponseStatus responseStatus) {
        responseBuilder.setResponseStatus(responseStatus);
    }

    public void contentType(String contentType) {
        responseBuilder.setHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    public boolean isContentType(String contentType) {
        return request.getHeaders().containsHeader(HttpHeaderNames.CONTENT_TYPE) && contentType != null &&
                request.getHeaders().get(HttpHeaderNames.CONTENT_TYPE).startsWith(contentType);
    }

    public void contentDisposition(String contentDisposition) {
        responseBuilder.setHeader(HttpHeaderNames.CONTENT_DISPOSITION, contentDisposition);
    }

    public void contentLength(int contentLength) {
        responseBuilder.setHeader(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(contentLength));
    }

    public void sendPermanentRedirect(String url) {
        responseBuilder.setResponseStatus(HttpResponseStatus.MOVED_PERMANENTLY);
        responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public void sendRedirect(String url) {
        responseBuilder.setResponseStatus(HttpResponseStatus.FOUND);
        responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public void seeOther(String url) {
        responseBuilder.setResponseStatus(HttpResponseStatus.SEE_OTHER);
        responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public void temporaryRedirect(String url) {
        responseBuilder.setResponseStatus(HttpResponseStatus.TEMPORARY_REDIRECT);
        responseBuilder.setHeader(HttpHeaderNames.LOCATION, url);
    }

    public String contextPath(String rel) {
        return urlProto(rel, false);
    }

    public String url(String rel) {
        return urlProto(rel, true);
    }

    public String urlProto(String rel, boolean absolute) {
        URL url = request.getServerURL().resolve(rel);
        logger.log(Level.FINE, "server URL = " + request.getServerURL() + " rel = " + rel + " --> " + url);
        return absolute ? url.toExternalForm() : toOrigin(url);
    }

    public String encodeUrl(String rel) {
        return encodeUrl(rel, true);
    }

    public String encodeUrl(String rel, boolean absolute) {
        URLBuilder builder = request.getServerURL().resolve(rel).mutator();
        if (session != null) {
            if (getModel().containsKey("session.url.enabled") && getModel().containsKey("session.url.parametername")) {
                String sessionIdParameterName = (String) getModel().get("session.url.parametername");
                builder.queryParam(sessionIdParameterName, session.id());
            }
        }
        URL url = builder.build();
        return absolute ? url.toExternalForm() : toOrigin(url);
    }

    public static String toOrigin(URL url) {
        StringBuilder sb = new StringBuilder();
        if (url.getPath() != null) {
            sb.append(url.getPath());
        }
        if (url.getQuery() != null) {
            sb.append('?').append(url.getQuery());
        }
        if (url.getFragment() != null) {
            sb.append('#').append(url.getFragment());
        }
        if (sb.length() == 0) {
            sb.append('/');
        }
        return sb.toString();
    }

    public String encodeUriTemplate(String spec, Map<String, Object> vars) {
        URITemplate uriTemplate = new URITemplate(spec);
        Variables.Builder builder = Variables.builder();
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return uriTemplate.toURL(builder.build()).toString();
    }

    public void write(String string) {
        if (string != null) {
            responseBuilder.write(string);
        }
    }

    public void writeBytes(byte[] bytes) {
        responseBuilder.write(DefaultDataBufferFactory.getInstance().wrap(bytes));
    }

    public String randomKey() {
        return RandomUtil.randomString(16);
    }

    public String fullDateTimeNow() {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)
                .withLocale(application.getLocale())
                .withZone(application.getZoneId());
        return ZonedDateTime.now().format(formatter);
    }

    public String bootstrapCss() {
        return contextPath("webjars/bootstrap/3.4.1/dist/css/bootstrap.min.css");
    }

    public String bootstrapJs() {
        return contextPath("webjars/bootstrap/3.4.1/dist/js/bootstrap.min.js");
    }

    public String jqueryJs() {
        return contextPath("webjars/jquery/3.5.1/dist/jquery.min.js");
    }

    public String fontawesomeCss() {
        return contextPath("webjars/font-awesome/5.14.0/css/all.min.css");
    }

    public String fontawesomeJs() {
        return contextPath("webjars/font-awesome/5.14.0/js/all.min.js");
    }

    public String popperJs() {
        return contextPath("webjars/popper.js/1.14.4/umd/popper.min.js");
    }

    public String fileinputCss() {
        return contextPath("webjars/bootstrap-fileinput/4.4.8/css/fileinput.min.css");
    }

    public String fileinputJs() {
        return contextPath("webjars/bootstrap-fileinput/4.4.8/js/fileinput.min.js");
    }

    public String fileinputLocale(String locale) {
        return contextPath("webjars/bootstrap-fileinput/4.4.8/js/locales/" + locale + ".js");
    }

    public String fileinputTheme(String theme) {
        return contextPath("webjars/bootstrap-fileinput/4.4.8/themes/" + theme + "/theme.min.js");
    }

    public String datatablesCss() {
        return contextPath("webjars/datatables/1.10.19/css/jquery.dataTables.min.css");
    }

    public String datatablesJs() {
        return contextPath("webjars/datatables/1.10.19/js/jquery.dataTables.min.js");
    }

    public String bootstrapTableCss() {
        return contextPath("webjars/bootstrap-table/1.15.4/dist/bootstrap-table.min.css");
    }

    public String bootstrapTableLocale(String locale) {
        return contextPath("webjars/bootstrap-table/1.15.4/dist/locale/bootstrap-table-${locale}.min.js");
    }

    public String bootstrapTableJs() {
        return contextPath("webjars//bootstrap-table/1.15.4/dist/bootstrap-table.min.js");
    }

    public String bootstrapTableAutoRefreshJs() {
        return contextPath("webjars/bootstrap-table/1.15.4/dist/extensions/auto-refresh/bootstrap-table-auto-refresh.min.js");
    }

    public String bootstrapHoverDropdownJs() {
        return contextPath("webjars/bootstrap-hover-dropdown/2.2.1/bootstrap-hover-dropdown.min.js");
    }

    public String bootstrapValidatorJs() {
        return contextPath("webjars/bootstrap-validator/0.11.9/js/validator.js");
    }
}
