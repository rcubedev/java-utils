package com.github.rcubedev.example.event.impl.bus.registry;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.spi.Registrar;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.example.event.impl.subscription.SubscriptionFactory;
import com.github.rcubedev.example.event.impl.bus.registry.HandlerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class RegistrationSession<B extends Event> implements Registrar<B> {
    private final List<BatchedSubscription> subscriptions;
    private final HandlerRegistry<B> registry;
    private final SubscriptionFactory<B> factory;

    public RegistrationSession(HandlerRegistry<B> registry, SubscriptionFactory<B> factory, List<BatchedSubscription> subscriptions) {
        this.registry = registry;
        this.factory = factory;
        this.subscriptions = subscriptions;
    }

    @Override
    public @NotNull <E extends B> Subscription register(Class<E> type, Priority priority, EventProcessor<E> processor) {
        BatchedSubscription sub = this.factory.createBatched(type, priority, processor);
        this.registry.add(type, priority, processor, sub);
        this.subscriptions.add(sub);
        return sub;
    }
}