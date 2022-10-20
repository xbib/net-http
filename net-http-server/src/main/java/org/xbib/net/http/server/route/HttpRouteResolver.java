package org.xbib.net.http.server.route;

import java.util.Set;
import org.xbib.net.Parameter;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpMethod;

import java.util.List;

public interface HttpRouteResolver<T> {

    void resolve(HttpRoute route, ResultListener<T> listener);

    interface Builder<T> {

        Builder<T> add(HttpRoute route, T value);

        Builder<T> add(HttpAddress httpAddress, HttpMethod httpMethod, String path, T value);

        Builder<T> add(HttpAddress httpAddress, Set<HttpMethod> httpMethods, String path, T value);

        Builder<T> sort(boolean sort);

        HttpRouteResolver<T> build();
    }

    interface Result<T> {

        T getValue();

        List<String> getContext();

        Parameter getParameter();

    }

    @FunctionalInterface
    interface ResultListener<T> {

        void onResult(Result<T> result);

    }
}
