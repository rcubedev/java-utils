package com.github.rcubedev.example.event.impl.subscription;

import com.github.rcubedev.example.event.api.spi.Subscription;

import java.util.function.Consumer;

public class BasicSubscription implements Subscription {
    private final Consumer<Subscription> unregisterAction;
    private volatile boolean unsubscribed = false;
    private final Object lock = new Object();

    public BasicSubscription(Consumer<Subscription> unregisterAction) {
        this.unregisterAction = unregisterAction;
    }

    @Override
    public void unsubscribe() {
        if (unsubscribed) return;
        synchronized (lock) {
            if (unsubscribed) return;
            unsubscribed = true;
        }
        unregisterAction.accept(this);
    }
}
