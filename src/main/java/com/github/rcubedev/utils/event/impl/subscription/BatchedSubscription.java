package com.github.rcubedev.utils.event.impl.subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.rcubedev.utils.event.api.spi.Subscription;

public class BatchedSubscription implements Subscription {
    private final Predicate<Subscription> unregisterAction;
    private final Consumer<Subscription> unsubscribeAction;
    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);

    public BatchedSubscription(Predicate<Subscription> unregisterAction, Consumer<Subscription> unsubscribeAction) {
        this.unregisterAction = unregisterAction;
        this.unsubscribeAction = unsubscribeAction;
    }

    @Override
    public void unsubscribe() {
        // can't test CAS false branch easily: guards CAS race
        if (unsubscribed.get() || !unsubscribed.compareAndSet(false, true)) return;
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
        if (unsubscribed.get()) return false;
        // can't test CAS false branch easily: guards CAS race
        return unsubscribed.compareAndSet(false, true) && unregisterAction.test(this);
    }
}
