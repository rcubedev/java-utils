package com.github.rcubedev.example.event.impl.subscription;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.spi.Linkable;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.bus.registry.HandlerRegistry;
import com.github.rcubedev.example.event.impl.bus.dispatch.Dispatcher;
import com.github.rcubedev.example.event.impl.subscription.factory.BasicSubscriptionFactory;
import com.github.rcubedev.example.event.impl.subscription.factory.BatchedSubscriptionFactory;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class SubscriptionFactory<B extends Event> {
    private final HandlerRegistry<B> registry;
    private final Dispatcher<B> dispatcher;
    private final BasicSubscriptionFactory basicFactory;
    private final BatchedSubscriptionFactory batchedFactory;

    @UnitTestIgnored
    public SubscriptionFactory(HandlerRegistry<B> registry, Dispatcher<B> dispatcher) {
        this(registry, dispatcher, BasicSubscription::new, BatchedSubscription::new);
    }

    SubscriptionFactory(HandlerRegistry<B> registry, Dispatcher<B> dispatcher, BasicSubscriptionFactory basicFactory,
                        BatchedSubscriptionFactory batchedFactory) {
        this.registry = registry;
        this.dispatcher = dispatcher;
        this.basicFactory = basicFactory;
        this.batchedFactory = batchedFactory;
    }

    public <E extends B> @NotNull Subscription createBasic(@NotNull Class<E> type, @NotNull Priority priority, @NotNull EventProcessor<E> listener) {
        Consumer<Subscription> unregister = sub ->
                dispatcher.update(() -> this.registry.remove(type, priority, sub) ? this.registry.snapshot() : null);

        return link(basicFactory.create(unregister), listener);
    }

    public <E extends B> @NotNull BatchedSubscription createBatched(@NotNull Class<E> type, @NotNull Priority priority, @NotNull EventProcessor<E> listener) {
        Predicate<Subscription> removeInternal = sub -> this.registry.remove(type, priority, sub);

        Consumer<Subscription> standaloneUnsubscribe = s ->
                this.dispatcher.update(() -> removeInternal.test(s) ? this.registry.snapshot() : null);

        return link(batchedFactory.create(removeInternal, standaloneUnsubscribe), listener);
    }

    private <T extends Subscription> T link(T sub, Object listener) {
        if (listener instanceof Linkable linkable) linkable.setSubscription(sub);
        return sub;
    }
}
