package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.impl.EventBusRegistry;

/**
 * Base class for all events.
 * @implNote If cancellable, implement {@link Cancellable}
 */
public abstract class Event {

    /**
     * Dispatch this event to all registered buses in the {@link EventBusRegistry}
     * that are compatible with this event's type.
     * <p>
     * This is a convenience method for global broadcasting.<br>
     * For targeted dispatch, use {@link com.github.rcubedev.example.event.api.spi.IEventBus#post(Event) IEventBus#post(Event)}
     * on a specific bus instance.
     */
    public final void dispatch() {
        EventBusRegistry.dispatch(this);
    }
}