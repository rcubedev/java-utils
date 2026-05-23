package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.spi.Subscription;

public final class MasterSubscription implements Subscription {
    private final BatchedSubscription[] children;
    private final Runnable rebuild;
    private volatile boolean unsubscribed = false;
    private final Object lock = new Object();

    public MasterSubscription(BatchedSubscription[] children, Runnable rebuild) {
        this.children = children;
        this.rebuild = rebuild;
    }

    @Override
    public void unsubscribe() {
        if (unsubscribed) return;
        synchronized (lock) {
            if (unsubscribed) return;
            unsubscribed = true;
        }
        boolean anyRemoved = false;
        for (BatchedSubscription child : children) {
            if (child.unregister()) anyRemoved = true;
        }
        if (anyRemoved) rebuild.run();
    }
}