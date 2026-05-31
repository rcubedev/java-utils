package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlattenerTest {

    @Mock
    private RegistrySnapshot<Event> mockSnapshot;

    @Mock
    private RegisteredParentResolver<Event> mockResolver;

    @Mock
    private HierarchyFallbackResolverFactory<Event> mockFallbackFactory;

    @Mock
    private DispatchTableFactory<Event> mockTableFactory;

    @Mock
    private HierarchyFallbackResolver<Event> mockFallbackResolver;

    @Mock
    private DispatchTable<Event> mockDispatchTable;

    @Mock
    private EventSinkSnapshot<Event> mockSink1;

    @Mock
    private EventSinkSnapshot<Event> mockSink2;

    @Mock
    private EventProcessor<Event> mockInvoker1;

    @Mock
    private EventProcessor<Event> mockInvoker2;

    private Flattener<Event> flattener;

    static class SuperEvent extends TestEvent {}
    static class ChildEvent extends TestEvent {}

    @BeforeEach
    void setUp() {
        flattener = new Flattener<>(mockSnapshot, mockResolver, mockFallbackFactory, mockTableFactory);
    }

    @Test
    void testFlattenWithEmptyFamiliesList() {
        when(mockSnapshot.getHandlers()).thenReturn(Collections.emptyMap());

        when(mockFallbackFactory.create(eq(mockResolver), anyMap()))
                .thenReturn(mockFallbackResolver);

        when(mockTableFactory.create(anyMap(), eq(mockFallbackResolver)))
                .thenReturn(mockDispatchTable);

        DispatchTable<Event> table = flattener.flatten(Collections.emptyList());

        assertNotNull(table);
        assertSame(mockDispatchTable, table);
    }

    @Test
    void testFlattenSkipsEmptyFamilyLineages() {
        when(mockSnapshot.getHandlers()).thenReturn(Collections.emptyMap());

        List<List<Class<? extends Event>>> families = new ArrayList<>();
        families.add(Collections.emptyList());

        when(mockFallbackFactory.create(eq(mockResolver), anyMap()))
                .thenReturn(mockFallbackResolver);

        when(mockTableFactory.create(anyMap(), eq(mockFallbackResolver)))
                .thenReturn(mockDispatchTable);

        DispatchTable<Event> table = flattener.flatten(families);

        assertNotNull(table);
        assertSame(mockDispatchTable, table);
    }

    @Test
    void testFlattenWithPopulatedFamiliesAndHandlers() {
        Map<Priority, EventSinkSnapshot<? extends Event>> superPriorityMap = new HashMap<>();
        superPriorityMap.put(Priority.NORMAL, mockSink1);

        Map<Priority, EventSinkSnapshot<? extends Event>> childPriorityMap = new HashMap<>();
        childPriorityMap.put(Priority.HIGH, mockSink2);

        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new LinkedHashMap<>();
        handlersMap.put(SuperEvent.class, superPriorityMap);
        handlersMap.put(ChildEvent.class, childPriorityMap);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        when(mockSink1.invoker()).thenReturn(mockInvoker1);
        when(mockSink2.invoker()).thenReturn(mockInvoker2);

        List<Class<? extends Event>> family = List.of(SuperEvent.class, ChildEvent.class);
        List<List<Class<? extends Event>>> families = List.of(family);

        when(mockFallbackFactory.create(eq(mockResolver), anyMap())).thenAnswer(invocation -> {
            Map<Class<? extends Event>, EventProcessor<? super Event>[]> pool = invocation.getArgument(1);
            assertTrue(pool.containsKey(ChildEvent.class));

            EventProcessor<? super Event>[] processors = pool.get(ChildEvent.class);
            assertNotNull(processors);

            assertEquals(2, processors.length);
            assertEquals(mockInvoker1, processors[0]); // Priority.NORMAL
            assertEquals(mockInvoker2, processors[1]); // Priority.HIGH
            return mockFallbackResolver;
        });

        when(mockTableFactory.create(anyMap(), eq(mockFallbackResolver)))
                .thenReturn(mockDispatchTable);

        DispatchTable<Event> table = flattener.flatten(families);

        assertNotNull(table);
        assertSame(mockDispatchTable, table);
    }

    @Test
    void testFlattenHandlesMissingPriorityMapsGracefully() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new LinkedHashMap<>();
        handlersMap.put(SuperEvent.class, null);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);

        List<Class<? extends Event>> family = List.of(SuperEvent.class);
        List<List<Class<? extends Event>>> families = List.of(family);

        when(mockFallbackFactory.create(eq(mockResolver), anyMap()))
                .thenReturn(mockFallbackResolver);

        when(mockTableFactory.create(anyMap(), eq(mockFallbackResolver)))
                .thenReturn(mockDispatchTable);

        DispatchTable<Event> table = flattener.flatten(families);

        assertNotNull(table);
        assertSame(mockDispatchTable, table);
    }
}
