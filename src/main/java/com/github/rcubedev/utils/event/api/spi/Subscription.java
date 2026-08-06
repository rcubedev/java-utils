package com.github.rcubedev.utils.event.api.spi;

/**
 * A handle to an active event registration.
 * <p>
 * Calling {@link #unsubscribe()} or {@link #close()} removes the associated 
 * listeners from the bus.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}