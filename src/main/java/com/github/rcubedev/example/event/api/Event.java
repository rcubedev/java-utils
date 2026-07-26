package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.api.spi.IEventBus;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all events.
 * @implSpec If cancellable, implement {@link Cancellable}
 * @see IEventBus
 */
public abstract class Event {

    private final @NotNull EventBusRegistry registry;

    @UnitTestIgnored
    public Event() {
        this(EventBusRegistry.getInstance());
    }

    Event(@NotNull EventBusRegistry registry) {
        this.registry = registry;
    }

    /**
     * Dispatch this event to all registered buses in the {@link EventBusRegistry}
     * that are compatible with this event's type.
     * <p>
     * This is a convenience method for global broadcasting.<br>
     * For targeted dispatch, use {@link com.github.rcubedev.example.event.api.spi.IEventBus#post(Event) IEventBus#post(Event)}
     * on a specific bus instance.
     */
    public final void dispatch() {
        registry.dispatch(this);
    }
}