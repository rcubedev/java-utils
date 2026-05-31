package com.github.rcubedev.example.event.api.spi;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class SubscriptionTests {

    @Test
    void close_ShouldDelegateToUnsubscribe() {
        AtomicBoolean unsubscribedCalled = new AtomicBoolean(false);
        Subscription subscription = () -> unsubscribedCalled.set(true);

        subscription.close();

        assertTrue(unsubscribedCalled.get(), "close() should have invoked unsubscribe()");
    }
}