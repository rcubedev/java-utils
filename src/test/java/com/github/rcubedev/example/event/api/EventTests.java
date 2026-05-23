package com.github.rcubedev.example.event.api;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class EventTests {

    @Test
    void dispatch_ShouldRouteEventToProvidedRegistry() {
        EventBusRegistry mockRegistry = mock(EventBusRegistry.class);
        TestEvent event = new TestEvent(mockRegistry);

        event.dispatch();
        verify(mockRegistry, times(1)).dispatch(event);
    }
}