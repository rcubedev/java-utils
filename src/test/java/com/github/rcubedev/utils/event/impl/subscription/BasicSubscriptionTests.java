package com.github.rcubedev.utils.event.impl.subscription;

import com.github.rcubedev.utils.event.api.spi.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicSubscriptionTests {

    @Mock
    private Consumer<Subscription> mockUnregisterAction;

    @Test
    void unsubscribe_ShouldInvokeAction_WhenCalledForFirstTime() {
        BasicSubscription subscription = new BasicSubscription(mockUnregisterAction);

        subscription.unsubscribe();

        verify(mockUnregisterAction, times(1)).accept(subscription);
    }

    @Test
    void unsubscribe_ShouldShortCircuit_WhenCalledMultipleTimes() {
        BasicSubscription subscription = new BasicSubscription(mockUnregisterAction);

        subscription.unsubscribe();
        subscription.unsubscribe();

        verify(mockUnregisterAction, times(1)).accept(subscription);
    }
}