package com.github.rcubedev.example.event.impl.bus.registry.factory;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.bus.handler.ArrayBackedEventSink;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// fixme this leaks state: RegistrySnapshot
@ExtendWith(MockitoExtension.class)
class RegistrySnapshotFactoryTests {

    private RegistrySnapshotFactory<Event> factory;

    @BeforeEach
    void setUp() {
        factory = new RegistrySnapshotFactory<>();
    }

    @Test
    void create_ShouldPopulateSnapshotMap_WhenSinksExist() {
        Map<Class<? extends Event>, Map<Priority, ArrayBackedEventSink<? extends Event>>> rawHandlers = new HashMap<>();
        Map<Priority, ArrayBackedEventSink<? extends Event>> priorityMap = new EnumMap<>(Priority.class);

        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<Event> mockSink = mock(ArrayBackedEventSink.class);
        @SuppressWarnings("unchecked")
        EventSinkSnapshot<Event> mockSinkSnapshot = mock(EventSinkSnapshot.class);
        when(mockSink.snapshot()).thenReturn(mockSinkSnapshot);
        
        priorityMap.put(Priority.NORMAL, mockSink);
        rawHandlers.put(TestEvent.class, priorityMap);

        RegistrySnapshot<Event> result = factory.create(rawHandlers);

        assertNotNull(result);
        verify(mockSink).snapshot();
    }

    @Test
    void create_ShouldSkipNullPriorityMaps_HittingFirstContinueBranch() {
        Map<Class<? extends Event>, Map<Priority, ArrayBackedEventSink<? extends Event>>> rawHandlers = new HashMap<>();
        rawHandlers.put(Event.class, null);

        RegistrySnapshot<Event> result = factory.create(rawHandlers);
        
        assertNotNull(result);
    }

    @Test
    void create_ShouldSkipEmptyPriorityMaps_HittingSecondContinueBranch() {
        Map<Class<? extends Event>, Map<Priority, ArrayBackedEventSink<? extends Event>>> rawHandlers = new HashMap<>();
        rawHandlers.put(TestEvent.class, new EnumMap<>(Priority.class));

        RegistrySnapshot<Event> result = factory.create(rawHandlers);
        
        assertNotNull(result);
    }
}