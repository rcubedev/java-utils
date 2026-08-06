package com.github.rcubedev.utils.event.impl.bus.registry;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.utils.event.impl.subscription.SubscriptionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class RegistrationBuffer<B extends Event> implements Registrar<B> {
    private final List<BatchedSubscription> subscriptions;
    private final SubscriptionFactory<B> factory;
    private final List<StagedEntry<B, ?>> stagedEntries = new ArrayList<>();

    public RegistrationBuffer(SubscriptionFactory<B> factory, List<BatchedSubscription> subscriptions) {
        this.factory = factory;
        this.subscriptions = subscriptions;
    }

    @Override
    public @NotNull <E extends B> Subscription register(Class<E> type, Priority priority, EventProcessor<E> processor) {
        BatchedSubscription sub = this.factory.createBatched(type, priority, processor);
        this.subscriptions.add(sub);
        this.stagedEntries.add(new StagedEntry<>(type, priority, processor, sub));
        return sub;
    }

    public void flush(HandlerRegistry<B> registry) {
        for (StagedEntry<B, ?> entry : this.stagedEntries) entry.apply(registry);
    }

    private record StagedEntry<B extends Event, E extends B>(Class<E> type, Priority priority,
                                                             EventProcessor<E> processor, Subscription subscription) {
        public void apply(HandlerRegistry<B> registry) {
            registry.add(type(), priority(), processor(), subscription());
        }
    }

    @FunctionalInterface
    public interface Factory<B extends Event> {
        RegistrationBuffer<B> create(SubscriptionFactory<B> factory, List<BatchedSubscription> subscriptions);
    }
}
