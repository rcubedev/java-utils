package com.github.rcubedev.example.event.impl.bus;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.spi.*;
import com.github.rcubedev.example.event.impl.bus.factory.RegistrationSessionFactory;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrationSession;
import com.github.rcubedev.example.event.impl.subscription.factory.MasterSubscriptionFactory;
import com.github.rcubedev.example.event.impl.subscription.SubscriptionFactory;
import com.github.rcubedev.example.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.example.event.impl.subscriber.EventSubscriberCompiler;
import com.github.rcubedev.example.event.impl.subscription.MasterSubscription;
import com.github.rcubedev.example.event.impl.bus.dispatch.Dispatcher;
import com.github.rcubedev.example.event.impl.bus.registry.HandlerRegistry;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class EventBus<B extends Event> implements IEventBus<B> {

    private final Class<B> busType;
    private final HandlerRegistry<B> registry;
    private final Dispatcher<B> dispatcher;
    private final SubscriptionFactory<B> factory;
    private final EventSubscriberCompiler<B> compiler;
    private final RegistrationSessionFactory<B> sessionFactory;
    private final MasterSubscriptionFactory masterSubFactory;

    @UnitTestIgnored
    public EventBus(Class<B> busType, int maxStackDepth) {
        this(busType, new HandlerRegistry<>(), new Dispatcher<>(busType, maxStackDepth));
    }

    @UnitTestIgnored
    private EventBus(Class<B> busType, HandlerRegistry<B> registry, Dispatcher<B> dispatcher) {
        this(busType, registry, dispatcher, new SubscriptionFactory<>(registry, dispatcher),
                new EventSubscriberCompiler<>(busType), RegistrationSession::new, MasterSubscription::new);
    }

    EventBus(Class<B> busType, HandlerRegistry<B> registry, Dispatcher<B> dispatcher, SubscriptionFactory<B> factory,
             EventSubscriberCompiler<B> compiler, RegistrationSessionFactory<B> sessionFactory,
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
    public @NotNull <E extends B> Subscription register(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        Subscription sub = this.factory.createBasic(eventType, priority, listener);

        dispatcher.update(() -> {
            this.registry.add(eventType, priority, listener, sub);
            return this.registry.snapshot();
        });
        return sub;
    }

    @Override
    public @NotNull Subscription register(Object target) {
        List<BatchedSubscription> subscriptions = new ArrayList<>();

        this.dispatcher.update(() -> {
            // todo i don't really want to lock the whole time while the methods are compiling.
            RegistrationSession<B> registrar = this.sessionFactory.create(this.registry, this.factory, subscriptions);
            this.compiler.register(target, registrar);
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
