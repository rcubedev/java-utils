package com.github.rcubedev.example.event.impl.subscription;

import com.github.rcubedev.example.event.api.spi.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class BasicSubscription implements Subscription {
    private final Consumer<Subscription> unregisterAction;
    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);

    public BasicSubscription(Consumer<Subscription> unregisterAction) {
        this.unregisterAction = unregisterAction;
    }

    @Override
    public void unsubscribe() {
        // can't test the CAS false branch easily: unreachable in single-threaded context, guards CAS race
        if (unsubscribed.get() || !unsubscribed.compareAndSet(false, true)) return;
        unregisterAction.accept(this);
    }
}
