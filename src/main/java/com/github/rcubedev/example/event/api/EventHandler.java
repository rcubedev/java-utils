package com.github.rcubedev.example.event.api;

/**
 * Base class for event handlers.
 */
public abstract class EventHandler<E extends Event> {

    /**
     * Register a listener to the event ({@link E}), for the specified priority
     *
     * @param listener the desired listener.
     */
    public abstract void register(Priority priority, EventProcessor<E> listener);

    /**
     * Register a listener to the event ({@link E}), in the normal {@link Priority}.
     *
     * @param listener the desired listener.
     */
    public abstract void register(EventProcessor<E> listener);

    /**
     * Get the invoker for this event handler.
     *
     * @return the invoker processor
     */
    public abstract EventProcessor<E> invoker();
}