package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

/**
 * Event that can be cancelled. Once cancelled it cannot be uncancelled.
 */
public abstract class CancellableEvent extends Event implements Cancellable {

    private volatile boolean cancelled = false;

    @UnitTestIgnored
    public CancellableEvent() {
        super();
    }

    CancellableEvent(@NotNull EventBusRegistry registry) {
        super(registry);
    }

    /**
     * Check if event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancel the event. Cannot be undone
     */
    @Override
    public void cancel() {
        cancelled = true;
    }
}