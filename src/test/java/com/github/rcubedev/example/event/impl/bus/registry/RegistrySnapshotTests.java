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
        // Arrange
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> outerMap = new HashMap<>();
        Map<Priority, EventSinkSnapshot<? extends Event>> innerMap = new EnumMap<>(Priority.class);

        @SuppressWarnings("unchecked")
        EventSinkSnapshot<TestEvent> mockSink = mock(EventSinkSnapshot.class);
        innerMap.put(Priority.NORMAL, mockSink);
        outerMap.put(TestEvent.class, innerMap);

        // Act
        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(outerMap);

        // Assert: It is a copy (Modifying the original outer map shouldn't affect snapshot)
        outerMap.remove(TestEvent.class);
        assertTrue(snapshot.getHandlers().containsKey(TestEvent.class),
                "Snapshot should retain data after original outer map is mutated");

        // Assert: Inner maps are copied (Modifying the original inner map shouldn't affect snapshot)
        innerMap.clear();
        Map<Priority, EventSinkSnapshot<? extends Event>> snapshotInner = snapshot.getHandlers().get(TestEvent.class);
        assertNotNull(snapshotInner);
        assertTrue(snapshotInner.containsKey(Priority.NORMAL),
                "Snapshot inner map should retain data after original inner map is cleared");

        // Assert: Outer map is immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            snapshot.getHandlers().put(TestEvent.class, Map.of());
        }, "Outer map should be unmodifiable");

        // Assert: Inner maps are immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            snapshotInner.put(Priority.HIGH, mockSink);
        }, "Inner maps should be unmodifiable");
    }

    @Test
    void constructor_ShouldOptimizeEmptyInnerMapsToMapOf() {
        // Arrange
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> outerMap = new HashMap<>();
        outerMap.put(TestEvent.class, Collections.emptyMap());

        // Act
        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(outerMap);

        // Assert: Verifies the `v.isEmpty()` optimization line
        Map<Priority, EventSinkSnapshot<? extends Event>> snapshotInner = snapshot.getHandlers().get(TestEvent.class);
        assertNotNull(snapshotInner);
        assertEquals(0, snapshotInner.size());

        // Map.of() throws UOE on mutations
        assertThrows(UnsupportedOperationException.class, () -> {
            @SuppressWarnings("unchecked")
            EventSinkSnapshot<TestEvent> mockSink = mock(EventSinkSnapshot.class);
            snapshotInner.put(Priority.NORMAL, mockSink);
        });
    }

    @Test
    void getHandlers_ShouldReturnStoredMap() {
        // Arrange
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> map = Map.of();

        // Act
        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(map);

        // Assert
        assertNotNull(snapshot.getHandlers());
        assertEquals(0, snapshot.getHandlers().size());
    }
}