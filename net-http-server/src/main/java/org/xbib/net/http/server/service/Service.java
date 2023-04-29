package org.xbib.net.http.server.service;

import org.xbib.net.http.server.HttpServerConfig;
import org.xbib.net.http.server.route.HttpRouterContext;
import org.xbib.net.http.server.decorate.Unwrappable;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Service extends Unwrappable {

    default void serviceAdded(HttpServerConfig cfg) throws Exception {
    }

    void serve(HttpRouterContext ctx) throws Exception;

    /**
     * Unwraps this {@link Service} into the object of the specified {@code type}.
     * Use this method instead of an explicit downcast. For example:
     * <pre>{@code
     * HttpService s = new MyService().decorate(LoggingService.newDecorator())
     *                                .decorate(AuthService.newDecorator());
     * MyService s1 = s.as(MyService.class);
     * LoggingService s2 = s.as(LoggingService.class);
     * AuthService s3 = s.as(AuthService.class);
     * }</pre>
     *
     * @param type the type of the object to return
     * @return the object of the specified {@code type} if found, or {@code null} if not found.
     *
     * @see Unwrappable
     */
    @Override
    default <T> T as(Class<T> type) {
        requireNonNull(type, "type");
        return Unwrappable.super.as(type);
    }

    /**
     * Unwraps this {@link Service} and returns the object being decorated.
     * If this {@link Service} is the innermost object, this method returns itself.
     * For example:
     * <pre>{@code
     * HttpService service1 = new MyService();
     * assert service1.unwrap() == service1;
     *
     * HttpService service2 = service1.decorate(LoggingService.newDecorator());
     * HttpService service3 = service2.decorate(AuthService.newDecorator());
     * assert service2.unwrap() == service1;
     * assert service3.unwrap() == service2;
     * assert service3.unwrap().unwrap() == service1;
     * }</pre>
     */
    @Override
    default Service unwrap() {
        return (Service) Unwrappable.super.unwrap();
    }
}
