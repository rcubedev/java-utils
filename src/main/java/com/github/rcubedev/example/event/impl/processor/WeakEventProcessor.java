package com.github.rcubedev.example.event.impl.processor;

import java.lang.ref.WeakReference;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.spi.Linkable;
import com.github.rcubedev.example.event.api.spi.Subscription;

// todo impl this in eventsubscriberhandler
//  this might be moved to api pkg.
public final class WeakEventProcessor<T, E extends Event> implements EventProcessor<E>, Linkable {
    private final WeakReference<T> targetRef;
    private final UnboundProcessor<T, E> invoker; // this should be implemented via metafactory if using EventSubscriberHandler
    private volatile Subscription subscription; // Set after registration

    public WeakEventProcessor(T target, UnboundProcessor<T, E> invoker) {
        this.targetRef = new WeakReference<>(target);
        this.invoker = invoker;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void process(E event) {
        Subscription sub = this.subscription;
        if (sub == null) throw new IllegalStateException("Event registered before subscription set.");

        T target = targetRef.get();
        if (target != null) {
            invoker.process(target, event);
            return;
        }

        // Target is gc'd. Self-unsubscribe.
        sub.unsubscribe();
    }
}
