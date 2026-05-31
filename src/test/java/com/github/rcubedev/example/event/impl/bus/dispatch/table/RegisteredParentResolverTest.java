package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisteredParentResolverTest {

    @Mock
    private RegistrySnapshot<Event> mockSnapshot;

    @Mock
    private EventSinkSnapshot<Event> mockSink;

    private RegisteredParentResolver<Event> resolver;

    static class SuperEvent extends TestEvent {}
    static class ChildEvent extends SuperEvent {}

    static class UnregisteredSuperEvent extends TestEvent {}
    static class UnregisteredChildEvent extends UnregisteredSuperEvent {}

    static abstract class TestBusRoot extends TestEvent {}
    static class TestBusEvent extends TestBusRoot {}
    static class NonMatchingEvent extends TestEvent {}

    @BeforeEach
    void setUp() {
        resolver = new RegisteredParentResolver<>(Event.class, mockSnapshot);
    }

    @Test
    void testGetRegisteredEventHierarchyFiltersUnregisteredTypes() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();

        handlersMap.put(SuperEvent.class, dummyPriorityMap);
        handlersMap.put(ChildEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        Class<? super ChildEvent>[] hierarchy = resolver.getRegisteredEventHierarchy(ChildEvent.class);

        assertNotNull(hierarchy);
        assertTrue(hierarchy.length >= 2);
        assertEquals(ChildEvent.class, hierarchy[0]);
        assertEquals(SuperEvent.class, hierarchy[1]);

        List<Class<? super ChildEvent>> hierarchyList = Arrays.asList(hierarchy);
        assertFalse(hierarchyList.contains(UnregisteredChildEvent.class));
    }

    @Test
    void testGetRegisteredEventHierarchyTrueAndFalseBranch() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();

        handlersMap.put(UnregisteredChildEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        Class<? super UnregisteredChildEvent>[] hierarchy = resolver.getRegisteredEventHierarchy(UnregisteredChildEvent.class);

        assertNotNull(hierarchy);
        assertTrue(hierarchy.length >= 1);
        assertEquals(UnregisteredChildEvent.class, hierarchy[0]);
        assertFalse(Arrays.asList(hierarchy).contains(UnregisteredSuperEvent.class));
    }

    @Test
    void testGetRegisteredEventHierarchyConditionOneFalseShortCircuit() {
        @SuppressWarnings("unchecked")
        RegistrySnapshot<TestBusRoot> specializedSnapshot = (RegistrySnapshot<TestBusRoot>) (RegistrySnapshot<?>) mockSnapshot;
        RegisteredParentResolver<TestBusRoot> specializedResolver =
                new RegisteredParentResolver<>(TestBusRoot.class, specializedSnapshot);

        when(mockSnapshot.getHandlers()).thenReturn(Collections.emptyMap());

        Class<? super TestBusEvent>[] hierarchy = specializedResolver.getRegisteredEventHierarchy(TestBusEvent.class);

        assertNotNull(hierarchy);
    }

    @Test
    void testGetRegisteredParentWhenTypeIsItselfRegistered() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();
        handlersMap.put(SuperEvent.class, dummyPriorityMap);
        handlersMap.put(ChildEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        Class<? super ChildEvent> parent = resolver.getRegisteredParent(ChildEvent.class);

        assertNotNull(parent);
        assertEquals(SuperEvent.class, parent);
    }

    @Test
    void testGetRegisteredParentWhenTypeItselfIsUnregistered() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();
        handlersMap.put(SuperEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        Class<? super ChildEvent> parent = resolver.getRegisteredParent(ChildEvent.class);

        assertNotNull(parent);
        assertEquals(SuperEvent.class, parent);
    }

    @Test
    void testGetRegisteredParentReturnsNullWhenNoAncestorsAreRegistered() {
        when(mockSnapshot.getHandlers()).thenReturn(Collections.emptyMap());

        Class<? super SuperEvent> parent = resolver.getRegisteredParent(SuperEvent.class);

        assertNull(parent);
    }

    @Test
    void testGetRegisteredParentAsExtendsBusCastsCorrectly() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();
        handlersMap.put(SuperEvent.class, dummyPriorityMap);
        handlersMap.put(ChildEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        Class<? extends Event> parent = resolver.getRegisteredParentAsExtendsBus(ChildEvent.class);

        assertNotNull(parent);
        assertEquals(SuperEvent.class, parent);
    }

    @Test
    void testGetRegisteredParentWhenTypeIsRegisteredRootWithNoRegisteredAncestors() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();
        handlersMap.put(SuperEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        Class<? super SuperEvent> parent = resolver.getRegisteredParent(SuperEvent.class);

        assertNull(parent);
    }

    @Test
    void testHierarchyDepthReturnsCorrectCount() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();
        handlersMap.put(SuperEvent.class, dummyPriorityMap);
        handlersMap.put(ChildEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        int depthWithHandlers = resolver.hierarchyDepth(ChildEvent.class);
        int depthWithoutHandlers = resolver.hierarchyDepth(UnregisteredChildEvent.class);

        assertTrue(depthWithHandlers >= 2);
        assertEquals(0, depthWithoutHandlers);
    }

    @Test
    void testCacheHitsAvoidDuplicateSnapshotLookups() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new HashMap<>();
        handlersMap.put(SuperEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        resolver.getRegisteredEventHierarchy(SuperEvent.class);
        resolver.getRegisteredEventHierarchy(SuperEvent.class);

        verify(mockSnapshot, times(1)).getHandlers();
    }
}
