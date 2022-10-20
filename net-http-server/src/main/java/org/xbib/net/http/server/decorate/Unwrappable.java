package org.xbib.net.http.server.decorate;

import static java.util.Objects.requireNonNull;

/**
 * Provides a way to unwrap an object in decorator pattern, similar to down-casting in an inheritance pattern.
 */
public interface Unwrappable {
    /**
     * Unwraps this object into the object of the specified {@code type}.
     * Use this method instead of an explicit downcast. For example:
     * <pre>{@code
     * class Foo {}
     *
     * class Bar<T> extends AbstractWrapper<T> {
     *     Bar(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * class Qux<T> extends AbstractWrapper<T> {
     *     Qux(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * Qux qux = new Qux(new Bar(new Foo()));
     * Foo foo = qux.as(Foo.class);
     * Bar bar = qux.as(Bar.class);
     * }</pre>
     *
     * @param type the type of the object to return
     * @return the object of the specified {@code type} if found, or {@code null} if not found.
     */
    default <T> T as(Class<T> type) {
        requireNonNull(type, "type");
        return type.isInstance(this) ? type.cast(this) : null;
    }

    /**
     * Unwraps this object and returns the object being decorated. If this {@link Unwrappable} is the innermost
     * object, this method returns itself. For example:
     * <pre>{@code
     * class Foo implements Unwrappable {}
     *
     * class Bar<T extends Unwrappable> extends AbstractUnwrappable<T> {
     *     Bar(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * class Qux<T extends Unwrappable> extends AbstractUnwrappable<T> {
     *     Qux(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * Foo foo = new Foo();
     * assert foo.unwrap() == foo;
     *
     * Bar<Foo> bar = new Bar<>(foo);
     * assert bar.unwrap() == foo;
     *
     * Qux<Bar<Foo>> qux = new Qux<>(bar);
     * assert qux.unwrap() == bar;
     * assert qux.unwrap().unwrap() == foo;
     * }</pre>
     */
    default Unwrappable unwrap() {
        return this;
    }
}
