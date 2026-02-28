package com.github.rcubedev.example.event.api;

/**
 * Base class for all events.
 *
 */
public abstract class Event {

    /**
     * Returns the handler instance.
     *
     * @return The handler instance.
     */
    public abstract EventHandler<? extends Event> handler();

    /**
     * Returns the runtime class of this event.
     *
     * @return The event class.
     */
    public final Class<? extends Event> eventType() {
        return this.getClass();
    }

    /**
     * Dispatch this event to its handler.
     * This internally gets the {@linkplain EventProcessor invoker} from {@link #handler()}
     */
    @SuppressWarnings("unchecked")
    public final void dispatch() {
        EventHandler<Event> handler = (EventHandler<Event>) handler();
        handler.invoker().process(this);
    }
}