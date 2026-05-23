package com.github.rcubedev.example.event.impl.subscription.factory;

import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.subscription.BasicSubscription;

import java.util.function.Consumer;

@FunctionalInterface
public interface BasicSubscriptionFactory {

    BasicSubscription create(Consumer<Subscription> unregisterAction);
}
