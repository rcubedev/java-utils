package com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HierarchyResolverTest {

    @Mock
    private RegisteredParentResolver<Event> mockResolver;

    @Mock
    private EventProcessor<Event> mockProcessor1;

    @Mock
    private EventProcessor<Event> mockProcessor2;

    private Map<Class<? extends Event>, List<EventProcessor<? super Event>>> preComputedPool;
    private HierarchyResolver<Event> fallbackResolver;

    static class TargetUnregisteredEvent extends TestEvent {}
    static class ResolvedParentEvent extends TestEvent {}

    @BeforeEach
    void setUp() {
        preComputedPool = new HashMap<>();
        fallbackResolver = new HierarchyResolver<>(mockResolver, preComputedPool);
    }

    @Test
    void testResolveWhenParentIsNullReturnsEmptyArray() {
        Class<?> unregisteredType = TargetUnregisteredEvent.class;

        when(mockResolver.getRegisteredParentAsExtendsBus(TargetUnregisteredEvent.class)).thenReturn(null);

        List<EventProcessor<? super Event>> result = fallbackResolver.resolve(unregisteredType);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testResolveWhenParentIsNotNullButMissingFromPoolReturnsEmptyArray() {
        Class<?> unregisteredType = TargetUnregisteredEvent.class;

        Mockito.<Class<?>>when(mockResolver.getRegisteredParentAsExtendsBus(TargetUnregisteredEvent.class)).thenReturn(ResolvedParentEvent.class);

        preComputedPool.put(ResolvedParentEvent.class, List.of());

        List<EventProcessor<? super Event>> result = fallbackResolver.resolve(unregisteredType);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testResolveWhenParentIsPresentInPoolReturnsPreComputedProcessors() {
        Class<?> unregisteredType = TargetUnregisteredEvent.class;

        List<EventProcessor<? super Event>> expectedProcessors = List.of(mockProcessor1, mockProcessor2);

        Mockito.<Class<?>>when(mockResolver.getRegisteredParentAsExtendsBus(TargetUnregisteredEvent.class)).thenReturn(ResolvedParentEvent.class);

        preComputedPool.put(ResolvedParentEvent.class, expectedProcessors);

        List<EventProcessor<? super Event>> result = fallbackResolver.resolve(unregisteredType);

        assertNotNull(result);
        assertSame(expectedProcessors, result);
        assertEquals(2, result.size());
        assertEquals(mockProcessor1, result.getFirst());
        assertEquals(mockProcessor2, result.get(1));
    }
}