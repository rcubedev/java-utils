package com.github.rcubedev.example.event.impl.bus.registry;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.bus.handler.ArrayBackedEventSink;
import com.github.rcubedev.example.event.impl.bus.registry.factory.EventSinkFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlerRegistryTests {

    private HandlerRegistry<Event> registry;

    @Mock private Subscription mockSub;
    @Mock private EventProcessor<TestEvent> mockProcessor;
    @Mock private RegistrySnapshot.Factory<Event> mockSnapshotFactory;
    @Mock private EventSinkFactory<Event> mockSinkFactory;
    @Mock private RegistrySnapshot<Event> mockSnapshot;

    @BeforeEach
    void setUp() {
        registry = new HandlerRegistry<>(mockSinkFactory, mockSnapshotFactory);
    }

    @Test
    void add_ShouldInstantiateAndCallHandler() {
        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<TestEvent> mockSink = mock(ArrayBackedEventSink.class);
        when(mockSinkFactory.create(TestEvent.class, Priority.HIGH)).thenReturn(mockSink);

        registry.add(TestEvent.class, Priority.HIGH, mockProcessor, mockSub);

        verify(mockSinkFactory).create(TestEvent.class, Priority.HIGH);
        verify(mockSink).addListener(mockProcessor, mockSub);
    }

    @Test
    void remove_ShouldDelegateToHandler_AndReturnItsResult() {
        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<TestEvent> mockSink = mock(ArrayBackedEventSink.class);
        when(mockSinkFactory.create(TestEvent.class, Priority.NORMAL)).thenReturn(mockSink);
        when(mockSink.removeListener(mockSub)).thenReturn(true);

        registry.add(TestEvent.class, Priority.NORMAL, mockProcessor, mockSub);

        boolean result = registry.remove(TestEvent.class, Priority.NORMAL, mockSub);

        assertTrue(result);
        verify(mockSink).removeListener(mockSub);
    }

    @Test
    void remove_ReturnsFalse_WhenNoHandlerExists() {
        boolean result = registry.remove(TestEvent.class, Priority.NORMAL, mockSub);
        assertFalse(result);
    }

    @Test
    void remove_ReturnsFalse_WhenEventTypeExistsButPriorityIsMissing() {
        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<TestEvent> mockSink = mock(ArrayBackedEventSink.class);
        when(mockSinkFactory.create(TestEvent.class, Priority.HIGH)).thenReturn(mockSink);

        registry.add(TestEvent.class, Priority.HIGH, mockProcessor, mockSub);

        boolean result = registry.remove(TestEvent.class, Priority.NORMAL, mockSub);

        assertFalse(result);
    }

    @Test
    void remove_ReturnsFalse_WhenHandlerSinksFailsToRemoveSubscription() {
        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<TestEvent> mockSink = mock(ArrayBackedEventSink.class);
        when(mockSinkFactory.create(TestEvent.class, Priority.NORMAL)).thenReturn(mockSink);
        when(mockSink.removeListener(mockSub)).thenReturn(false);

        registry.add(TestEvent.class, Priority.NORMAL, mockProcessor, mockSub);

        boolean result = registry.remove(TestEvent.class, Priority.NORMAL, mockSub);

        assertFalse(result);
        verify(mockSink).removeListener(mockSub);
    }

    @Test
    void snapshot_ShouldDelegateToSnapshotFactory() {
        when(mockSnapshotFactory.create(any())).thenReturn(mockSnapshot);

        RegistrySnapshot<Event> result = registry.snapshot();

        assertNotNull(result);
        assertEquals(mockSnapshot, result);
        verify(mockSnapshotFactory).create(any());
    }
}