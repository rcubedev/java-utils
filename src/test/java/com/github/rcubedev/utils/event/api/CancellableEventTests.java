package com.github.rcubedev.utils.event.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancellableEventTests {

    private static final class TestCancellableEvent extends CancellableEvent {
        public TestCancellableEvent(@NotNull EventBusRegistry registry) {
            super(registry);
        }
    }

    @Mock private EventBusRegistry mockRegistry;

    private TestCancellableEvent event;

    @BeforeEach
    void setUp() {
        event = new TestCancellableEvent(mockRegistry);
    }

    @Test
    void isCancelled_ByDefault_ShouldReturnFalse() {
        assertFalse(event.isCancelled(), "A freshly instantiated event should not be cancelled by default.");
    }

    @Test
    void cancel_WhenInvoked_ShouldSetCancelledToTrue() {
        event.cancel();

        assertTrue(event.isCancelled(), "Calling cancel() must transition the state to cancelled.");
    }

    @Test
    void cancel_CalledMultipleTimes_ShouldRemainCancelled() {
        event.cancel();
        event.cancel();

        assertTrue(event.isCancelled(), "Subsequent calls to cancel() should leave the event cancelled.");
    }

    @Test
    void dispatch_ShouldRouteToInjectedMockRegistry() {
        event.dispatch();
        verify(mockRegistry, times(1)).dispatch(event);
    }
}
