package com.github.rcubedev.example.event.api;

import org.jetbrains.annotations.NotNull;

/**
 * Dispatched whenever an event type is posted that contains zero listeners.
 */
public final class DeadEvent extends Event {
    private final Event event;

    public DeadEvent(@NotNull Event event) {
        this.event = event;
    }

    public @NotNull Event getEvent() {
        return this.event;
    }
}