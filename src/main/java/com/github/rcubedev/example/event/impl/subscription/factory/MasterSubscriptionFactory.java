package com.github.rcubedev.example.event.impl.subscription.factory;

import com.github.rcubedev.example.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.example.event.impl.subscription.MasterSubscription;

@FunctionalInterface
public interface MasterSubscriptionFactory {

    MasterSubscription create(BatchedSubscription[] children, Runnable rebuild);
}