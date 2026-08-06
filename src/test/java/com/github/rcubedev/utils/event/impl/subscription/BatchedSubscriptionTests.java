package com.github.rcubedev.utils.event.impl.subscription;

import com.github.rcubedev.utils.event.api.spi.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchedSubscriptionTests {

    @Mock private Predicate<Subscription> mockUnregisterAction;
    @Mock private Consumer<Subscription> mockUnsubscribeAction;

    @Test
    void unsubscribe_ShouldInvokeUnsubscribeAction_WhenCalledForFirstTime() {
        BatchedSubscription subscription = new BatchedSubscription(mockUnregisterAction, mockUnsubscribeAction);

        subscription.unsubscribe();

        verify(mockUnsubscribeAction, times(1)).accept(subscription);
    }

    @Test
    void unsubscribe_ShouldShortCircuit_WhenCalledMultipleTimes() {
        BatchedSubscription subscription = new BatchedSubscription(mockUnregisterAction, mockUnsubscribeAction);

        subscription.unsubscribe();
        subscription.unsubscribe();

        verify(mockUnsubscribeAction, times(1)).accept(subscription);
    }

    @Test
    void unregister_ShouldReturnFalse_WhenAlreadyUnsubscribed() {
        BatchedSubscription subscription = new BatchedSubscription(mockUnregisterAction, mockUnsubscribeAction);
        subscription.unsubscribe();

        boolean result = subscription.unregister();

        assertFalse(result);
        verify(mockUnregisterAction, never()).test(any());
    }

    @Test
    void unregister_ShouldReturnFalse_WhenAlreadyUnregistered() {
        BatchedSubscription subscription = new BatchedSubscription(mockUnregisterAction, mockUnsubscribeAction);
        subscription.unregister();

        boolean result = subscription.unregister();

        assertFalse(result);
        verify(mockUnregisterAction, times(1)).test(subscription);
    }

    @Test
    void unregister_ShouldInvokeUnregisterAction_WhenNotYetUnsubscribed() {
        when(mockUnregisterAction.test(any())).thenReturn(true);
        BatchedSubscription subscription = new BatchedSubscription(mockUnregisterAction, mockUnsubscribeAction);

        boolean result = subscription.unregister();

        assertTrue(result);
        verify(mockUnregisterAction, times(1)).test(subscription);
        verify(mockUnsubscribeAction, never()).accept(any());
    }

    @Test
    void unregister_ShouldNotInvokeUnsubscribeAction_WhenUnregisterActionReturnsFalse() {
        when(mockUnregisterAction.test(any())).thenReturn(false);
        BatchedSubscription subscription = new BatchedSubscription(mockUnregisterAction, mockUnsubscribeAction);

        boolean result = subscription.unregister();

        assertFalse(result);
        verify(mockUnsubscribeAction, never()).accept(any());
    }

    @Test
    void unsubscribe_ShouldShortCircuit_WhenPreviouslyUnregistered() {
        BatchedSubscription subscription = new BatchedSubscription(mockUnregisterAction, mockUnsubscribeAction);
        subscription.unregister();

        subscription.unsubscribe();

        verify(mockUnsubscribeAction, never()).accept(any());
    }
}