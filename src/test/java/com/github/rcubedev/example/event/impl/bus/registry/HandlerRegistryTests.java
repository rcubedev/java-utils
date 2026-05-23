package com.github.rcubedev.example.event.impl.bus.registry;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.bus.handler.ArrayBackedEventSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlerRegistryTests {

    private HandlerRegistry<Event> registry;

    @Mock private Subscription mockSub;
    @Mock private EventProcessor<TestEvent> mockProcessor;

    @BeforeEach
    void setUp() {
        registry = new HandlerRegistry<>();
    }

    @Test
    void add_ShouldInstantiateAndCallHandler() {
        try (MockedConstruction<ArrayBackedEventSink> mockedHandler = mockConstruction(ArrayBackedEventSink.class)) {

            registry.add(TestEvent.class, Priority.HIGH, mockProcessor, mockSub);

            assertEquals(1, mockedHandler.constructed().size());

            ArrayBackedEventSink<TestEvent> mock = mockedHandler.constructed().get(0);
            verify(mock).addListener(mockProcessor, mockSub);
        }
    }

    @Test
    void remove_ShouldDelegateToHandler_AndReturnItsResult() {
        try (MockedConstruction<ArrayBackedEventSink> mockedHandler = mockConstruction(ArrayBackedEventSink.class,
                (mock, context) ->
                // Tell the mock handler to return true when asked to remove
                when(mock.removeListener(any())).thenReturn(true))) {
            // Add it so the registry has a handler to call
            registry.add(TestEvent.class, Priority.NORMAL, mockProcessor, mockSub);

            boolean result = registry.remove(TestEvent.class, Priority.NORMAL, mockSub);

            assertTrue(result, "Registry should return the boolean from the handler");
            verify(mockedHandler.constructed().get(0)).removeListener(mockSub);
        }
    }

    @Test
    void snapshot_ShouldReturnMockedSnapshot() {
        // Intercept 'new RegistrySnapshot(...)'
        try (MockedConstruction<RegistrySnapshot> mockedSnapshot = mockConstruction(RegistrySnapshot.class)) {

            RegistrySnapshot<Event> result = registry.snapshot();

            assertNotNull(result);
            assertEquals(1, mockedSnapshot.constructed().size());
            assertEquals(mockedSnapshot.constructed().get(0), result);
        }
    }

    @Test
    void remove_ReturnsFalse_WhenNoHandlerExists() {
        // Pure isolation: No need for try-with-resources if we expect NO construction
        boolean result = registry.remove(TestEvent.class, Priority.NORMAL, mockSub);
        assertFalse(result);
    }
}