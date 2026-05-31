package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.spi.IEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBusRegistryImplTest {

    @Mock
    private IEventBus<Event> mockCompatibleBus;

    @Mock
    private IEventBus<SpecificEvent> mockIncompatibleBus;

    static class GeneralEvent extends TestEvent {}
    static class SpecificEvent extends TestEvent {}

    private EventBusRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new EventBusRegistryImpl();
    }

    @Test
    void testRegisterAndDispatchRouteOnlyToCompatibleBuses() {
        GeneralEvent event = new GeneralEvent();

        when(mockCompatibleBus.getBusType()).thenReturn(Event.class);
        when(mockIncompatibleBus.getBusType()).thenReturn(SpecificEvent.class);

        registry.register(mockCompatibleBus);
        registry.register(mockIncompatibleBus);

        registry.dispatch(event);

        verify(mockCompatibleBus, times(1)).post(event);
        verify(mockIncompatibleBus, never()).post(any());
    }

    @Test
    void testDispatchDoesNothingWhenNoBusesAreRegistered() {
        GeneralEvent event = new GeneralEvent();

        assertDoesNotThrow(() -> registry.dispatch(event));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRegisterAppendsMultipleBuses() {
        IEventBus<GeneralEvent> bus1 = mock(IEventBus.class);
        IEventBus<GeneralEvent> bus2 = mock(IEventBus.class);
        when(bus1.getBusType()).thenReturn(GeneralEvent.class);
        when(bus2.getBusType()).thenReturn(GeneralEvent.class);

        registry.register(bus1);
        registry.register(bus2);

        GeneralEvent event = new GeneralEvent();
        registry.dispatch(event);

        verify(bus1, times(1)).post(event);
        verify(bus2, times(1)).post(event);
    }

    @Test
    void testHolderInitializationForCoverage() {
        assertNotNull(EventBusRegistryImpl.Holder.INSTANCE);
    }
}