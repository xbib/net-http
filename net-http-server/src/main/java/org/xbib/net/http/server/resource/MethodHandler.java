package org.xbib.net.http.server.resource;

import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.HttpServerContext;

import java.lang.reflect.Method;

public class MethodHandler implements HttpHandler {

    private final Method m;

    private final Object obj;

    public MethodHandler(Method m, Object obj) throws IllegalArgumentException {
        this.m = m;
        this.obj = obj;
        Class<?>[] params = m.getParameterTypes();
        if (params.length != 1 ||
                !HttpRequest.class.isAssignableFrom(params[0]) ||
                !Void.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("invalid method signature: " + m);
        }
    }

    @Override
    public void handle(HttpServerContext context) {
        try {
            m.invoke(obj, context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
