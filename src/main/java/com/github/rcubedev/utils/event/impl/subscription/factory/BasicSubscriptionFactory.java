package com.github.rcubedev.utils.event.impl.subscription.factory;

import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.impl.subscription.BasicSubscription;

import java.util.function.Consumer;

@FunctionalInterface
public interface BasicSubscriptionFactory {

    BasicSubscription create(Consumer<Subscription> unregisterAction);
}
