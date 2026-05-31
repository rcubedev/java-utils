package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
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
class FamilyBuilderTest {

    @Mock
    private RegistrySnapshot<Event> mockSnapshot;

    @Mock
    private RegisteredParentResolver<Event> mockResolver;

    @Mock
    private EventSinkSnapshot<Event> mockSink;

    private FamilyBuilder<Event> builder;

    static class GrandchildEvent extends TestEvent.SubEvent {}
    static class SeparateEvent extends TestEvent {}

    @BeforeEach
    void setUp() {
        builder = new FamilyBuilder<>(mockSnapshot, mockResolver);
    }

    @Test
    void testBuildFamiliesWithEmptySnapshot() {
        when(mockSnapshot.getHandlers()).thenReturn(Collections.emptyMap());

        List<List<Class<? extends Event>>> result = builder.buildFamilies();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildFamiliesWithLinearInheritanceLineage() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);

        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new LinkedHashMap<>();
        handlersMap.put(TestEvent.class, dummyPriorityMap);
        handlersMap.put(TestEvent.SubEvent.class, dummyPriorityMap);
        handlersMap.put(GrandchildEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        when(mockResolver.hierarchyDepth(TestEvent.class)).thenReturn(1);
        when(mockResolver.hierarchyDepth(TestEvent.SubEvent.class)).thenReturn(2);
        when(mockResolver.hierarchyDepth(GrandchildEvent.class)).thenReturn(3);

        doReturn(null).when(mockResolver).getRegisteredParentAsExtendsBus(TestEvent.class);
        doReturn(TestEvent.class).when(mockResolver).getRegisteredParentAsExtendsBus(TestEvent.SubEvent.class);
        doReturn(TestEvent.SubEvent.class).when(mockResolver).getRegisteredParentAsExtendsBus(GrandchildEvent.class);

        List<List<Class<? extends Event>>> lineages = builder.buildFamilies();

        assertNotNull(lineages);
        assertEquals(3, lineages.size());
        assertEquals(List.of(TestEvent.class), lineages.get(0));
        assertEquals(List.of(TestEvent.class, TestEvent.SubEvent.class), lineages.get(1));
        assertEquals(List.of(TestEvent.class, TestEvent.SubEvent.class, GrandchildEvent.class), lineages.get(2));
    }

    @Test
    void testBuildFamiliesSkipsUnregisteredAncestorsInLineage() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);

        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new LinkedHashMap<>();
        handlersMap.put(TestEvent.class, dummyPriorityMap);
        handlersMap.put(GrandchildEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        when(mockResolver.hierarchyDepth(TestEvent.class)).thenReturn(1);
        when(mockResolver.hierarchyDepth(GrandchildEvent.class)).thenReturn(3);

        doReturn(null).when(mockResolver).getRegisteredParentAsExtendsBus(TestEvent.class);
        doReturn(TestEvent.class).when(mockResolver).getRegisteredParentAsExtendsBus(GrandchildEvent.class);

        List<List<Class<? extends Event>>> lineages = builder.buildFamilies();

        assertNotNull(lineages);
        assertEquals(2, lineages.size());
        assertEquals(List.of(TestEvent.class), lineages.get(0));
        assertEquals(List.of(TestEvent.class, GrandchildEvent.class), lineages.get(1));
    }

    @Test
    void testBuildFamiliesWithMutuallyIndependentBranches() {
        Map<Priority, EventSinkSnapshot<? extends Event>> dummyPriorityMap = Map.of(Priority.NORMAL, mockSink);

        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new LinkedHashMap<>();
        handlersMap.put(SeparateEvent.class, dummyPriorityMap);
        handlersMap.put(TestEvent.SubEvent.class, dummyPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        when(mockResolver.hierarchyDepth(SeparateEvent.class)).thenReturn(2);
        when(mockResolver.hierarchyDepth(TestEvent.SubEvent.class)).thenReturn(2);

        doReturn(null).when(mockResolver).getRegisteredParentAsExtendsBus(SeparateEvent.class);
        doReturn(null).when(mockResolver).getRegisteredParentAsExtendsBus(TestEvent.SubEvent.class);

        List<List<Class<? extends Event>>> lineages = builder.buildFamilies();

        assertNotNull(lineages);
        assertEquals(2, lineages.size());

        assertEquals(List.of(SeparateEvent.class), lineages.getFirst());
        assertEquals(List.of(TestEvent.SubEvent.class), lineages.getLast());
    }
}