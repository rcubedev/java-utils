package com.github.rcubedev.utils.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.TestEvent;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.CompositeResolver;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.DeadEventResolver;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.DirectPoolResolver;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.HierarchyResolver;
import com.github.rcubedev.utils.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.utils.event.impl.bus.registry.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private CompositeResolver.Factory<Event> mockCompositeFallbackFactory;

    @Mock
    private DirectPoolResolver.Factory<Event> mockDirectPoolFallbackFactory;

    @Mock
    private HierarchyResolver.Factory<Event> mockHierarchyFallbackFactory;

    @Mock
    private DeadEventResolver.Factory<Event> mockDeadEventFallbackFactory;

    @Mock
    private Flattener.Result.Factory mockResultFactory;

    @Mock
    private DirectPoolResolver<Event> mockDirectPoolResolver;

    @Mock
    private HierarchyResolver<Event> mockHierarchyResolver;

    @Mock
    private DeadEventResolver<Event> mockDeadEventResolver;

    @Mock
    private CompositeResolver<Event> mockCompositeResolver;

    @Mock
    private Flattener.Result<Event> mockResult;

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
        flattener = new Flattener<>(
                mockSnapshot,
                mockResolver,
                mockCompositeFallbackFactory,
                mockDirectPoolFallbackFactory,
                mockHierarchyFallbackFactory,
                mockDeadEventFallbackFactory,
                mockResultFactory
        );
    }

    private void setupFallbackStubbing() {
        when(mockDirectPoolFallbackFactory.create(anyMap()))
                .thenReturn(mockDirectPoolResolver);
        when(mockHierarchyFallbackFactory.create(eq(mockResolver), anyMap()))
                .thenReturn(mockHierarchyResolver);
        when(mockDeadEventFallbackFactory.create(anyMap()))
                .thenReturn(mockDeadEventResolver);
        when(mockCompositeFallbackFactory.create(eq(List.of(mockDirectPoolResolver, mockHierarchyResolver, mockDeadEventResolver))))
                .thenReturn(mockCompositeResolver);
    }

    @Test
    void testFlattenWithEmptyFamiliesList() {
        when(mockSnapshot.getHandlers()).thenReturn(Collections.emptyMap());
        setupFallbackStubbing();

        when(mockResultFactory.create(eq(mockCompositeResolver), anySet()))
                .thenReturn(mockResult);

        Flattener.Result<Event> result = flattener.flatten(Collections.emptyList());

        assertNotNull(result);
        assertSame(mockResult, result);
    }

    @Test
    void testFlattenSkipsEmptyFamilyLineages() {
        when(mockSnapshot.getHandlers()).thenReturn(Collections.emptyMap());
        setupFallbackStubbing();

        List<List<Class<? extends Event>>> families = new ArrayList<>();
        families.add(Collections.emptyList());

        when(mockResultFactory.create(eq(mockCompositeResolver), anySet()))
                .thenReturn(mockResult);

        Flattener.Result<Event> result = flattener.flatten(families);

        assertNotNull(result);
        assertSame(mockResult, result);
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

        when(mockDirectPoolFallbackFactory.create(anyMap()))
                .thenReturn(mockDirectPoolResolver);

        // Verify that the computed map context matches target handler maps accurately
        when(mockHierarchyFallbackFactory.create(eq(mockResolver), anyMap())).thenAnswer(invocation -> {
            Map<Class<? extends Event>, List<EventProcessor<? super Event>>> pool = invocation.getArgument(1);
            verifyPoolContents(pool);
            return mockHierarchyResolver;
        });

        when(mockDeadEventFallbackFactory.create(anyMap())).thenAnswer(invocation -> {
            Map<Class<? extends Event>, List<EventProcessor<? super Event>>> pool = invocation.getArgument(0);
            verifyPoolContents(pool);
            return mockDeadEventResolver;
        });

        when(mockCompositeFallbackFactory.create(eq(List.of(mockDirectPoolResolver, mockHierarchyResolver, mockDeadEventResolver))))
                .thenReturn(mockCompositeResolver);

        when(mockResultFactory.create(eq(mockCompositeResolver), anySet())).thenReturn(mockResult);

        Flattener.Result<Event> result = flattener.flatten(families);

        assertNotNull(result);
        assertSame(mockResult, result);
    }

    @Test
    void testFlattenHandlesMissingPriorityMapsGracefully() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new LinkedHashMap<>();
        handlersMap.put(SuperEvent.class, null);

        when(mockSnapshot.getHandlers()).thenReturn(handlersMap);
        setupFallbackStubbing();

        when(mockResultFactory.create(eq(mockCompositeResolver), anySet()))
                .thenReturn(mockResult);

        List<Class<? extends Event>> family = List.of(SuperEvent.class);
        List<List<Class<? extends Event>>> families = List.of(family);

        Flattener.Result<Event> result = flattener.flatten(families);

        assertNotNull(result);
        assertSame(mockResult, result);
    }

    private void verifyPoolContents(Map<Class<? extends Event>, List<EventProcessor<? super Event>>> pool) {
        assertTrue(pool.containsKey(ChildEvent.class));
        List<EventProcessor<? super Event>> processors = pool.get(ChildEvent.class);
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals(mockInvoker1, processors.getFirst()); // Priority.NORMAL
        assertEquals(mockInvoker2, processors.get(1)); // Priority.HIGH
    }
}