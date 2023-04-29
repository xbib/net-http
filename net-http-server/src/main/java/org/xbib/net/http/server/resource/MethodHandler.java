package org.xbib.net.http.server.resource;

import java.lang.reflect.Method;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.HttpRequest;
import org.xbib.net.http.server.route.HttpRouterContext;

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
    public void handle(HttpRouterContext context) {
        try {
            m.invoke(obj, context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
