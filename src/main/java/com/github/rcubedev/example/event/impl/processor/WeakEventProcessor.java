package com.github.rcubedev.example.event.impl.processor;

import java.lang.ref.WeakReference;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.spi.SubscriptionAware;
import com.github.rcubedev.example.event.api.spi.Subscription;

// todo make custom weakref that wraps a real weakref to ensure testability
public final class WeakEventProcessor<T, E extends Event> implements EventProcessor<E>, SubscriptionAware {

    private final WeakReference<T> targetRef;
    private final UnboundProcessor<T, E> invoker; // this should be implemented via metafactory if using EventSubscriberHandler
    private volatile Subscription subscription; // Set after registration. use StableValue in future

    public WeakEventProcessor(T target, UnboundProcessor<T, E> invoker) {
        this.targetRef = new WeakReference<>(target);
        this.invoker = invoker;
        //this.cleanable = Holder.CLEANER.register(target, this::clean); // fixme unsafe!! captures this so if user unsubscribes it will still be strongly ref'd
    }

    public void acceptSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void process(E event) {
        Subscription sub = this.subscription;
        if (sub == null) throw new IllegalStateException("Event handler registered before subscription linked.");

        // todo use Cleaner in private static
        T target = targetRef.get();
        if (target != null) {
            invoker.process(target, event);
            return;
        }

        // Target is gc'd. Self-unsubscribe.
        sub.unsubscribe();
    }

    public interface Factory<T, E extends Event> {
        WeakEventProcessor<T, E> create(T target, UnboundProcessor<T, E> invoker);
    }
}
