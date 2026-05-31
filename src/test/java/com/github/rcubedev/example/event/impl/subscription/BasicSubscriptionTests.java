package com.github.rcubedev.example.event.impl.subscription;

import com.github.rcubedev.example.event.api.spi.Subscription;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.junit.jupiter.api.Assertions.*;
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