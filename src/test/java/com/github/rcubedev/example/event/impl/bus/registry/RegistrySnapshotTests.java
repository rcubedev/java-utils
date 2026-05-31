package com.github.rcubedev.example.event.impl.bus.registry;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RegistrySnapshotTests {

    @Test
    void constructor_ShouldCreateDeepImmutableShallowCopy() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> outerMap = new HashMap<>();
        Map<Priority, EventSinkSnapshot<? extends Event>> innerMap = new EnumMap<>(Priority.class);

        @SuppressWarnings("unchecked")
        EventSinkSnapshot<TestEvent> mockSink = mock(EventSinkSnapshot.class);
        innerMap.put(Priority.NORMAL, mockSink);
        outerMap.put(TestEvent.class, innerMap);

        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(outerMap);

        outerMap.remove(TestEvent.class);
        assertTrue(snapshot.getHandlers().containsKey(TestEvent.class),
                "Snapshot should retain data after original outer map is mutated");

        innerMap.clear();
        Map<Priority, EventSinkSnapshot<? extends Event>> snapshotInner = snapshot.getHandlers().get(TestEvent.class);
        assertNotNull(snapshotInner);
        assertTrue(snapshotInner.containsKey(Priority.NORMAL),
                "Snapshot inner map should retain data after original inner map is cleared");

        assertThrows(UnsupportedOperationException.class, () -> {
            snapshot.getHandlers().put(TestEvent.class, Map.of());
        }, "Outer map should be unmodifiable");

        assertThrows(UnsupportedOperationException.class, () -> {
            snapshotInner.put(Priority.HIGH, mockSink);
        }, "Inner maps should be unmodifiable");
    }

    @Test
    void constructor_ShouldOptimizeEmptyInnerMapsToMapOf() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> outerMap = new HashMap<>();
        outerMap.put(TestEvent.class, Collections.emptyMap());

        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(outerMap);

        Map<Priority, EventSinkSnapshot<? extends Event>> snapshotInner = snapshot.getHandlers().get(TestEvent.class);
        assertNotNull(snapshotInner);
        assertEquals(0, snapshotInner.size());

        assertThrows(UnsupportedOperationException.class, () -> {
            @SuppressWarnings("unchecked")
            EventSinkSnapshot<TestEvent> mockSink = mock(EventSinkSnapshot.class);
            snapshotInner.put(Priority.NORMAL, mockSink);
        });
    }

    @Test
    void getHandlers_ShouldReturnStoredMap() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> map = Map.of();

        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(map);

        assertNotNull(snapshot.getHandlers());
        assertEquals(0, snapshot.getHandlers().size());
    }
}
