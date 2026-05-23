package com.github.rcubedev.example.event.impl;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.rcubedev.example.event.api.spi.Subscription;

public class BatchedSubscription implements Subscription {
    private final Predicate<Subscription> unregisterAction;
    private final Consumer<Subscription> unsubscribeAction;
    private volatile boolean unsubscribed = false;
    private final Object lock = new Object();

    public BatchedSubscription(Predicate<Subscription> unregisterAction, Consumer<Subscription> unsubscribeAction) {
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
        unsubscribeAction.accept(this);
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
        return unregisterAction.test(this);
    }
}
