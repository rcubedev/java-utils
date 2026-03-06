package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.impl.EventBusRegistry;

public final class EventDispatcher {
    private EventDispatcher() {}

    /**
     * Dispatches an event to all registered {@link EventBus} instances.
     * Each bus fires its listeners independently in priority order,
     * skipping buses whose base type the event is not an instance of.
     * <p>
     * This is equivalent to calling {@link Event#dispatch()} on the event itself.
     * Prefer {@link Event#dispatch()} when you have an event instance;
     * use this when dispatching from a context where the event type is generic.
     *
     * @param event The event to dispatch
     */
    public static void dispatch(Event event) {
        EventBusRegistry.dispatch(event);
    }
}