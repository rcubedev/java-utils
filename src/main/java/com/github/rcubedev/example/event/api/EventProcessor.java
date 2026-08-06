package com.github.rcubedev.example.event.api;

/**
 * Functional interface for processing an event.
 * <p>
 * Implementations may also implement {@link com.github.rcubedev.example.event.api.spi.SubscriptionAware SubscriptionAware}
 * to receive the subscription associated with their registration.
 *
 * @param <E> The event type
 */
@FunctionalInterface
public interface EventProcessor<E extends Event> {
    void process(E event);
}
