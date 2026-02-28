package com.github.rcubedev.example.event.api;

/**
 * Event that can be cancelled. Once cancelled it cannot be uncancelled.
 */
public abstract class CancellableEvent extends Event implements Cancellable {
    private volatile boolean cancelled = false;

    /**
     * {@inheritDoc}
     *
     * @return The handler instance.
     */
    @Override
    public abstract EventHandler<? extends CancellableEvent> handler();

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