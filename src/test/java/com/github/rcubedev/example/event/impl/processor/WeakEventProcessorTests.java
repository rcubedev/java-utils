package com.github.rcubedev.example.event.impl.processor;

import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.spi.Linkable;
import com.github.rcubedev.example.event.api.spi.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeakEventProcessorTests {

    @Mock private Subscription mockSubscription;
    @Mock private UnboundProcessor<Object, TestEvent> mockInvoker;

    @Test
    void process_ShouldThrow_WhenSubscriptionNotSet() {
        Object target = new Object();
        WeakEventProcessor<Object, TestEvent> processor = new WeakEventProcessor<>(target, mockInvoker);

        assertThrows(IllegalStateException.class, () -> processor.process(new TestEvent()));
    }

    @Test
    void process_ShouldInvokeProcessor_WhenTargetAlive() throws Exception {
        Object target = new Object();
        WeakEventProcessor<Object, TestEvent> processor = new WeakEventProcessor<>(target, mockInvoker);
        processor.setSubscription(mockSubscription);
        TestEvent event = new TestEvent();

        processor.process(event);

        verify(mockInvoker).process(target, event);
        verify(mockSubscription, never()).unsubscribe();
    }

    @Test
    void process_ShouldUnsubscribe_WhenTargetGarbageCollected() {
        WeakEventProcessor<Object, TestEvent> processor;
        {
            Object target = new Object();
            processor = new WeakEventProcessor<>(target, mockInvoker);
        }
        processor.setSubscription(mockSubscription);
        WeakEventProcessor<Object, TestEvent> finalProcessor = processor;

        // todo this kinda bad; we can create own own wrapping weak ref
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            while (true) {
                System.gc();
                Thread.sleep(10);
                finalProcessor.process(new TestEvent());
                try {
                    verify(mockSubscription, atLeastOnce()).unsubscribe();
                    return;
                } catch (AssertionError ignored) {}
            }
        });

        verify(mockInvoker, never()).process(any(), any());
    }

    @Test
    void setSubscription_ShouldImplementLinkable() {
        Object target = new Object();
        WeakEventProcessor<Object, TestEvent> processor = new WeakEventProcessor<>(target, mockInvoker);

        assertInstanceOf(Linkable.class, processor);
        assertDoesNotThrow(() -> processor.setSubscription(mockSubscription));
    }
}
