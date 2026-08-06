package com.github.rcubedev.utils.event.impl.bus.registry;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.TestEvent;
import com.github.rcubedev.utils.event.impl.bus.handler.ArrayBackedEventSink;
import com.github.rcubedev.utils.event.impl.bus.handler.EventSinkSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrySnapshotTests {

    @Test
    void constructor_ShouldCreateImmutableShallowCopy() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> outerMap = new HashMap<>();
        Map<Priority, EventSinkSnapshot<? extends Event>> innerMap = new EnumMap<>(Priority.class);

        @SuppressWarnings("unchecked")
        EventSinkSnapshot<TestEvent> mockSink = mock(EventSinkSnapshot.class);
        innerMap.put(Priority.NORMAL, mockSink);
        outerMap.put(TestEvent.class, innerMap);

        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(outerMap);

        assertThrows(UnsupportedOperationException.class, () -> snapshot.getHandlers().put(TestEvent.class, Map.of()),
                "Outer map should be unmodifiable");

        assertDoesNotThrow(() -> snapshot.getHandlers().get(TestEvent.class).put(Priority.HIGH, mockSink),
                "Direct constructor should only apply shallow unmodifiability wrapper to outer map");
    }

    @Test
    void factoryMethodCreate_ShouldDeeplyIsolateAndSnapshotSinks() {
        Map<Class<? extends Event>, Map<Priority, ArrayBackedEventSink<? extends Event>>> activeHandlers = new HashMap<>();
        Map<Priority, ArrayBackedEventSink<? extends Event>> priorityMap = new EnumMap<>(Priority.class);

        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<TestEvent> mockSink = mock(ArrayBackedEventSink.class);
        @SuppressWarnings("unchecked")
        EventSinkSnapshot<TestEvent> mockSnapshot = mock(EventSinkSnapshot.class);

        when(mockSink.snapshot()).thenReturn(mockSnapshot);

        priorityMap.put(Priority.NORMAL, mockSink);
        activeHandlers.put(TestEvent.class, priorityMap);

        RegistrySnapshot<Event> snapshot = RegistrySnapshot.create(activeHandlers);

        activeHandlers.remove(TestEvent.class);
        assertTrue(snapshot.getHandlers().containsKey(TestEvent.class), "Snapshot must retain data after source map is stripped");

        Map<Priority, EventSinkSnapshot<? extends Event>> snapshotInner = snapshot.getHandlers().get(TestEvent.class);
        assertNotNull(snapshotInner);
        assertSame(mockSnapshot, snapshotInner.get(Priority.NORMAL), "Snapshot must contain the baked sink wrapper result");

        verify(mockSink, times(1)).snapshot();

        assertThrows(UnsupportedOperationException.class, () -> snapshotInner.put(Priority.HIGH, mockSnapshot),
                "Inner maps produced by create() must be unmodifiable");
    }

    @Test
    void factoryMethodCreate_ShouldSkipNullOrEmptyInnerMaps() {
        Map<Class<? extends Event>, Map<Priority, ArrayBackedEventSink<? extends Event>>> activeHandlers = new HashMap<>();

        activeHandlers.put(TestEvent.class, null);

        activeHandlers.put(TestEvent.SubEvent.class, Collections.emptyMap());

        RegistrySnapshot<Event> snapshot = RegistrySnapshot.create(activeHandlers);

        assertFalse(snapshot.getHandlers().containsKey(TestEvent.class));
        assertFalse(snapshot.getHandlers().containsKey(TestEvent.SubEvent.class));
        assertTrue(snapshot.getHandlers().isEmpty());
    }

    @Test
    void factoryMethodFromSnapshots_ShouldCreateDeepImmutableShallowCopy() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> outerMap = new HashMap<>();
        Map<Priority, EventSinkSnapshot<? extends Event>> innerMap = new EnumMap<>(Priority.class);

        @SuppressWarnings("unchecked")
        EventSinkSnapshot<TestEvent> mockSink = mock(EventSinkSnapshot.class);
        innerMap.put(Priority.NORMAL, mockSink);
        outerMap.put(TestEvent.class, innerMap);

        RegistrySnapshot<Event> snapshot = RegistrySnapshot.createFromSnapshots(outerMap);

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
    void factoryMethodFromSnapshots_ShouldOptimizeEmptyInnerMapsViaRemoval() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> outerMap = new HashMap<>();
        outerMap.put(TestEvent.class, Collections.emptyMap());

        RegistrySnapshot<Event> snapshot = RegistrySnapshot.createFromSnapshots(outerMap);

        Map<Priority, EventSinkSnapshot<? extends Event>> snapshotInner = snapshot.getHandlers().get(TestEvent.class);
        assertNull(snapshotInner, "Empty inner maps should be stripped entirely from the snapshot container");
    }

    @Test
    void factoryMethodFromSnapshots_ShouldSkipNullKeysOrNullOrEmptyInnerMaps() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> snapshotMap = new HashMap<>();

        snapshotMap.put(null, Map.of());
        snapshotMap.put(TestEvent.class, null);
        snapshotMap.put(TestEvent.SubEvent.class, Collections.emptyMap());

        RegistrySnapshot<Event> snapshot = RegistrySnapshot.createFromSnapshots(snapshotMap);

        assertFalse(snapshot.getHandlers().containsKey(null));
        assertFalse(snapshot.getHandlers().containsKey(TestEvent.class));
        assertFalse(snapshot.getHandlers().containsKey(TestEvent.SubEvent.class));
        assertTrue(snapshot.getHandlers().isEmpty());
    }

    @Test
    void getHandlers_ShouldReturnStoredMap() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> map = Map.of();

        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(map);

        assertNotNull(snapshot.getHandlers());
        assertEquals(0, snapshot.getHandlers().size());
    }
}
