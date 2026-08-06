package com.github.rcubedev.utils.event.impl.subscription;

import com.github.rcubedev.utils.event.api.spi.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MasterSubscription implements Subscription {
    private final BatchedSubscription[] children;
    private final Runnable rebuild;
    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);

    public MasterSubscription(BatchedSubscription[] children, Runnable rebuild) {
        this.children = children;
        this.rebuild = rebuild;
    }

    @Override
    public void unsubscribe() {
        // CAS false branch can't be tested: guards CAS race
        if (unsubscribed.get() || !unsubscribed.compareAndSet(false, true)) return;
        boolean anyRemoved = false;
        for (BatchedSubscription child : children) {
            if (child.unregister()) anyRemoved = true;
        }
        if (anyRemoved) rebuild.run();
    }
}