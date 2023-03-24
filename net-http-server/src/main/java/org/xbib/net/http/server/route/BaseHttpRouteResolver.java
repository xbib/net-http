package org.xbib.net.http.server.route;

import java.util.Set;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.xbib.net.Parameter.Domain.PATH;

public class BaseHttpRouteResolver<T> implements HttpRouteResolver<T> {

    private static final Logger logger = Logger.getLogger(BaseHttpRouteResolver.class.getName());

    private final Builder<T> builder;

    private BaseHttpRouteResolver(Builder<T> builder) {
        this.builder = builder;
    }

    /**
     * This naive resolver walks through all configured routes and tries to match them.
     * @param httpRoute the route to match against
     * @param listener the listener where the results are going
     */
    @Override
    public void resolve(HttpRoute httpRoute, ResultListener<T> listener) {
        for (Map.Entry<HttpRoute, T> entry : builder.routes) {
            ParameterBuilder parameterBuilder = Parameter.builder().domain(PATH);
            boolean match = entry.getKey().matches(parameterBuilder, httpRoute);
            if (match && listener != null) {
                String path = httpRoute.getEffectivePath();
                List<String> list = Arrays.stream(path.split("/"))
                                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
                listener.onResult(new Result<>(entry.getValue(), list, parameterBuilder.build()));
            }
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Result<T> implements HttpRouteResolver.Result<T> {

        private final T value;

        private final List<String> context;

        private final Parameter parameter;

        Result(T value, List<String> context, Parameter parameter) {
            this.value = value;
            this.context = context;
            this.parameter = parameter;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public List<String> getContext() {
            return context;
        }

        @Override
        public Parameter getParameter() {
            return parameter;
        }
    }

    public static class Builder<T> implements HttpRouteResolver.Builder<T> {

        private final RouteComparator<T> comparator;

        private final List<Map.Entry<HttpRoute, T>> routes;

        private boolean sort;

        private Builder() {
            this.comparator = new RouteComparator<>();
            this.routes = new ArrayList<>();
            this.sort = false;
        }

        @Override
        public HttpRouteResolver.Builder<T> add(HttpAddress httpAddress, HttpMethod httpMethod, String prefix, String path, T value) {
            add(new BaseHttpRoute(httpAddress, Set.of(httpMethod), prefix, path, false), value);
            return this;
        }

        @Override
        public HttpRouteResolver.Builder<T> add(HttpAddress httpAddress, Set<HttpMethod> httpMethods, String prefix, String path, T value) {
            add(new BaseHttpRoute(httpAddress, httpMethods, prefix, path, false), value);
            return this;
        }

        @Override
        public HttpRouteResolver.Builder<T> add(HttpRoute httpRoute, T value) {
            routes.add(Map.entry(httpRoute, value));
            return this;
        }

        @Override
        public HttpRouteResolver.Builder<T> sort(boolean sort) {
            this.sort = sort;
            return this;
        }

        @Override
        public BaseHttpRouteResolver<T> build() {
            if (sort) {
                routes.sort(comparator);
            }
            return new BaseHttpRouteResolver<>(this);
        }
    }

    private static class RouteComparator<T> implements Comparator<Map.Entry<HttpRoute, T>> {

        @Override
        public int compare(Map.Entry<HttpRoute, T> o1, Map.Entry<HttpRoute, T> o2) {
            return o2.getKey().getSortKey().compareTo(o1.getKey().getSortKey());
        }
    }
}
