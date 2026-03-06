package com.github.rcubedev.example.event.api;

/**
 * Functional interface for processing an event.
 *
 * @param <E> The event type
 */
@FunctionalInterface
public interface EventProcessor<E extends Event> {
    void process(E event);
}