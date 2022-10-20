package org.xbib.net.http.server.route;

import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.Application;
import org.xbib.net.http.server.HttpDomain;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpRequestBuilder;
import org.xbib.net.http.server.HttpResponseBuilder;
import org.xbib.net.http.server.HttpServerContext;

import java.util.Collection;

public interface HttpRouter {

    Collection<HttpDomain> getDomains();

    DomainsByAddress getDomainsByAddress();

    void setApplication(Application application);

    void route(HttpRequestBuilder requestBuilder, HttpResponseBuilder responseBuilder);

    void routeException(HttpException e);

    void routeStatus(HttpResponseStatus httpResponseStatus, HttpServerContext httpServerContext);

    void routeToErrorHandler(HttpServerContext httpServerContext, Throwable t);
}
