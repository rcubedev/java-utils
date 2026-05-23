package com.github.rcubedev.example.event.impl.subscription.factory;

import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.subscription.BatchedSubscription;

import java.util.function.Consumer;
import java.util.function.Predicate;

@FunctionalInterface
public interface BatchedSubscriptionFactory {

    BatchedSubscription create(Predicate<Subscription> unregisterAction, Consumer<Subscription> unsubscribeAction);
}
