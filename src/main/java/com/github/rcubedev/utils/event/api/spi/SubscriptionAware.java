package com.github.rcubedev.utils.event.api.spi;

/**
 * A component that can receive its associated {@link Subscription}.
 *
 * @apiNote Implementations of {@link IEventBus} must provide the subscription created
 *          during registration to components implementing this interface.
 * @see Subscription
 */
public interface SubscriptionAware {

    /**
     * Associates this component with a subscription.
     *
     * @param subscription the associated subscription
     */
    void acceptSubscription(Subscription subscription);
}
