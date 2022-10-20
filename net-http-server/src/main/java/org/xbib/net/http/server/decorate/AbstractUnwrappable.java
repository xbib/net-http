package org.xbib.net.http.server.decorate;

import java.util.Objects;

/**
 * Skeletal {@link Unwrappable} implementation.
 *
 * @param <T> the type of the object being decorated
 */
public abstract class AbstractUnwrappable<T extends Unwrappable> implements Unwrappable {

    private final T delegate;

    /**
     * Creates a new decorator with the specified delegate.
     */
    protected AbstractUnwrappable(T delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public final <U> U as(Class<U> type) {
        final U result = Unwrappable.super.as(type);
        return result != null ? result : delegate.as(type);
    }

    @Override
    public T unwrap() {
        return delegate;
    }

    @Override
    public String toString() {
        final String simpleName = getClass().getSimpleName();
        final String name = simpleName.isEmpty() ? getClass().getName() : simpleName;
        return name + '(' + delegate + ')';
    }
}
