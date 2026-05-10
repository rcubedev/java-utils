package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.spi.Subscription;

public class BasicSubscription implements Subscription {
    private final Runnable unregisterAction;
    private volatile boolean unsubscribed = false;
    private final Object lock = new Object();

    public BasicSubscription(Runnable unregisterAction) {
        this.unregisterAction = unregisterAction;
    }

    @Override
    public void unsubscribe() {
        if (unsubscribed) return;
        synchronized (lock) {
            if (unsubscribed) return;
            unsubscribed = true;
        }
        unregisterAction.run();
    }
}
