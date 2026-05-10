package com.github.rcubedev.example.event.impl;

import java.util.function.BooleanSupplier;

import com.github.rcubedev.example.event.api.spi.Subscription;

public class BatchedSubscription implements Subscription {
    private final BooleanSupplier unregisterAction;
    private final Runnable unsubscribeAction;
    private volatile boolean unsubscribed = false;
    private final Object lock = new Object();

    public BatchedSubscription(BooleanSupplier unregisterAction, Runnable unsubscribeAction) {
        this.unregisterAction = unregisterAction;
        this.unsubscribeAction = unsubscribeAction;
    }

    @Override
    public void unsubscribe() {
        if (unsubscribed) return;
        synchronized (lock) {
            if (unsubscribed) return;
            unsubscribed = true;
        }
        unsubscribeAction.run();
    }

    /**
     * Remove this listener from its handler without triggering a rebuild.<br>
     * Must be followed by an external rebuild call.
     *
     * @return {@code true} if this call actually performed the removal,
     *         {@code false} if already unsubscribed
     */
    public boolean unregister() {
        if (unsubscribed) return false;
        synchronized (lock) {
            if (unsubscribed) return false;
            unsubscribed = true;
        }
        return unregisterAction.getAsBoolean();
    }
}
