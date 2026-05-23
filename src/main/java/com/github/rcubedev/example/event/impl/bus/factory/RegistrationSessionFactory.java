package com.github.rcubedev.example.event.impl.bus.factory;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.impl.bus.registry.HandlerRegistry;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrationSession;
import com.github.rcubedev.example.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.example.event.impl.subscription.SubscriptionFactory;

import java.util.List;

@FunctionalInterface
public interface RegistrationSessionFactory<B extends Event> {
    RegistrationSession<B> create(HandlerRegistry<B> registry, SubscriptionFactory<B> factory, List<BatchedSubscription> subscriptions);
}