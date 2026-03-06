package com.github.rcubedev.example.event.api;

/**
 * Base class for all events.
 */
public abstract class Event {

    /**
     * Dispatch this event to all registered buses who accept this event type.
     * Each bus fires its listeners independently in priority order.
     */
    public final void dispatch() {
        EventDispatcher.dispatch(this);
    }
}