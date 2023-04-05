package org.xbib.net.http.server.route;

import java.util.Collection;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.application.Application;
import org.xbib.net.http.server.domain.HttpDomain;

public interface HttpRouter {

    Collection<HttpDomain> getDomains();

    DomainsByAddress getDomainsByAddress();

    void route(Application application, HttpRequestBuilder requestBuilder, HttpResponseBuilder responseBuilder);

    void routeStatus(HttpResponseStatus httpResponseStatus, HttpServerContext httpServerContext);

    void routeToErrorHandler(HttpServerContext httpServerContext, Throwable t);

    void routeException(HttpException e);

}
