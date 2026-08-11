package com.github.rcubedev.utils.event.impl.bus;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.spi.*;
import com.github.rcubedev.utils.event.impl.bus.registry.RegistrationBuffer;
import com.github.rcubedev.utils.event.impl.subscription.factory.MasterSubscriptionFactory;
import com.github.rcubedev.utils.event.impl.subscription.SubscriptionFactory;
import com.github.rcubedev.utils.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.utils.event.impl.subscriber.EventSubscriberCompiler;
import com.github.rcubedev.utils.event.impl.subscription.MasterSubscription;
import com.github.rcubedev.utils.event.impl.bus.dispatch.Dispatcher;
import com.github.rcubedev.utils.event.impl.bus.registry.HandlerRegistry;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class EventBus<B extends Event> implements IEventBus<B> {

    private final Class<B> busType;
    private final HandlerRegistry<B> registry;
    private final Dispatcher<B> dispatcher;
    private final SubscriptionFactory<B> factory;
    private final EventSubscriberCompiler<B> compiler;
    private final RegistrationBuffer.Factory<B> sessionFactory;
    private final MasterSubscriptionFactory masterSubFactory;

    @UnitTestIgnored
    public EventBus(Class<B> busType, int maxStackDepth, boolean recursionGuardEnabled) {
        this(busType, new HandlerRegistry<>(), new Dispatcher<>(busType, maxStackDepth, recursionGuardEnabled));
    }

    @UnitTestIgnored
    private EventBus(Class<B> busType, HandlerRegistry<B> registry, Dispatcher<B> dispatcher) {
        this(busType, registry, dispatcher, new SubscriptionFactory<>(registry, dispatcher),
                new EventSubscriberCompiler<>(), RegistrationBuffer::new, MasterSubscription::new);
    }

    EventBus(Class<B> busType, HandlerRegistry<B> registry, Dispatcher<B> dispatcher, SubscriptionFactory<B> factory,
             EventSubscriberCompiler<B> compiler, RegistrationBuffer.Factory<B> sessionFactory,
             MasterSubscriptionFactory masterSubFactory) {
        this.busType = busType;
        this.registry = registry;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.compiler = compiler;
        this.sessionFactory = sessionFactory;
        this.masterSubFactory = masterSubFactory;
    }

    @Override
    public @NotNull Class<B> getBusType() {
        return this.busType;
    }

    @Override
    public <E extends B> void post(E event) {
        dispatcher.dispatch(event);
    }

    @Override
    public @NotNull RecursionBypass openBypassTo(int extraBudget) {
        return this.dispatcher.openBypassTo(extraBudget);
    }

    @Override
    public @NotNull <E extends B> Subscription register(Class<E> eventType, Priority priority, EventProcessor<E> listener, Identity identity) {
        Subscription sub = this.factory.createBasic(eventType, priority, listener);

        dispatcher.update(() -> {
            this.registry.add(eventType, priority, listener, sub);
            return this.registry.snapshot();
        });
        return sub;
    }

    @Override
    public @NotNull Subscription register(Object target, Identity identity) {
        List<BatchedSubscription> subscriptions = new ArrayList<>();
        RegistrationBuffer<B> registrar = this.sessionFactory.create(this.busType, this.factory, subscriptions);
        this.compiler.build(target, identity, registrar);

        this.dispatcher.update(() -> {
            registrar.flush(this.registry);
            return this.registry.snapshot();
        });

        return this.masterSubFactory.create(
                subscriptions.toArray(BatchedSubscription[]::new),
                this::rebuildSnapshot);
    }

    private void rebuildSnapshot() {
        dispatcher.update(registry::snapshot);
    }
}
