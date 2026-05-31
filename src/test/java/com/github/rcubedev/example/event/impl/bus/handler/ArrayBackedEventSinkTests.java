package com.github.rcubedev.example.event.impl.bus.handler;

import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.spi.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArrayBackedEventSinkTests {

    private ArrayBackedEventSink<TestEvent> handler;
    @Mock private EventSinkSnapshot.Factory<TestEvent> mockSnapshotFactory;
    @Mock private EventSinkSnapshot<TestEvent> mockSnapshot;

    @BeforeEach
    void setUp() {
        handler = new ArrayBackedEventSink<>(TestEvent.class, Priority.NORMAL, mockSnapshotFactory);
    }

    @Test
    void testMetadata() {
        assertEquals(TestEvent.class, handler.eventType());
        assertEquals(Priority.NORMAL, handler.priority());
    }

    @Nested
    class InvokerRebuildLogic {

        @Test
        void invoker_Empty_DoesNothing() {
            EventProcessor<TestEvent> invoker = handler.invoker();
            // Should not throw or crash
            assertDoesNotThrow(() -> invoker.process(new TestEvent()));
        }

        @Test
        void invoker_SingleListener_CallsDirectly() {
            AtomicInteger calls = new AtomicInteger(0);
            EventProcessor<TestEvent> listener = e -> calls.incrementAndGet();

            handler.addListener(listener, mock(Subscription.class));

            handler.invoker().process(new TestEvent());
            assertEquals(1, calls.get());
        }

        @Test
        void invoker_MultipleListeners_CallsAllInOrder() {
            StringBuilder sb = new StringBuilder();
            EventProcessor<TestEvent> first = e -> sb.append("1");
            EventProcessor<TestEvent> second = e -> sb.append("2");
            EventProcessor<TestEvent> third = e -> sb.append("3");

            handler.addListener(first, mock(Subscription.class));
            handler.addListener(second, mock(Subscription.class));
            handler.addListener(third, mock(Subscription.class));

            handler.invoker().process(new TestEvent());
            assertEquals("123", sb.toString());
        }
    }

    @Nested
    class ListenerManagement {

        @Test
        void removeListener_ReturnsFalseIfNotFound() {
            assertFalse(handler.removeListener(mock(Subscription.class)));
        }

        @Test
        void removeListener_HandlesVariousPositions() {
            Subscription s1 = mock(Subscription.class);
            Subscription s2 = mock(Subscription.class);
            Subscription s3 = mock(Subscription.class);
            AtomicInteger calls = new AtomicInteger(0);
            EventProcessor<TestEvent> p = e -> calls.incrementAndGet();

            handler.addListener(p, s1);
            handler.addListener(p, s2);
            handler.addListener(p, s3);

            // Remove middle (s2)
            assertTrue(handler.removeListener(s2));
            handler.invoker().process(new TestEvent());
            assertEquals(2, calls.get());

            // Remove start (s1)
            calls.set(0);
            assertTrue(handler.removeListener(s1));
            handler.invoker().process(new TestEvent());
            assertEquals(1, calls.get());

            // Remove last remaining (s3)
            calls.set(0);
            assertTrue(handler.removeListener(s3));
            handler.invoker().process(new TestEvent());
            assertEquals(0, calls.get());
        }

        @Test
        void removeListener_ForcesArrayBoundsConditionalCopy() {
            Subscription s1 = mock(Subscription.class);
            Subscription s2 = mock(Subscription.class);
            Subscription s3 = mock(Subscription.class);
            EventProcessor<TestEvent> p = e -> {};

            handler.addListener(p, s1);
            handler.addListener(p, s2);
            handler.addListener(p, s3);

            // Explicitly tests 'index < listeners.length - 1' branch scenarios safely
            assertTrue(handler.removeListener(s2));
            assertTrue(handler.removeListener(s3));
        }

        @Test
        void clear_RemovesAllAndResetsInvoker() {
            AtomicInteger calls = new AtomicInteger(0);
            handler.addListener(e -> calls.incrementAndGet(), mock(Subscription.class));
            handler.addListener(e -> calls.incrementAndGet(), mock(Subscription.class));

            handler.clear();

            handler.invoker().process(new TestEvent());
            assertEquals(0, calls.get());
            // Verify that adding again works after clear
            handler.addListener(e -> calls.incrementAndGet(), mock(Subscription.class));
            handler.invoker().process(new TestEvent());
            assertEquals(1, calls.get());
        }
    }

    @Nested
    class SnapshotLogic {

        @Test
        void snapshot_ShouldInvokeFactoryAndReturnInstance() {
            when(mockSnapshotFactory.snapshot(eq(TestEvent.class), eq(Priority.NORMAL), any()))
                    .thenReturn(mockSnapshot);

            EventSinkSnapshot<TestEvent> result = handler.snapshot();

            assertNotNull(result);
            assertEquals(mockSnapshot, result);
            verify(mockSnapshotFactory, times(1)).snapshot(eq(TestEvent.class), eq(Priority.NORMAL), any());
        }
    }
}