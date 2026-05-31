package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HierarchyFallbackResolverTest {

    @Mock
    private RegisteredParentResolver<Event> mockResolver;

    @Mock
    private EventProcessor<Event> mockProcessor1;

    @Mock
    private EventProcessor<Event> mockProcessor2;

    private Map<Class<? extends Event>, EventProcessor<? super Event>[]> preComputedPool;
    private HierarchyFallbackResolver<Event> fallbackResolver;

    static class TargetUnregisteredEvent extends TestEvent {}
    static class ResolvedParentEvent extends TestEvent {}

    @BeforeEach
    void setUp() {
        preComputedPool = new HashMap<>();
        fallbackResolver = new HierarchyFallbackResolver<>(mockResolver, preComputedPool);
    }

    @Test
    void testResolveWhenParentIsNullReturnsEmptyArray() {
        Class<?> unregisteredType = TargetUnregisteredEvent.class;

        when(mockResolver.getRegisteredParentAsExtendsBus(TargetUnregisteredEvent.class)).thenReturn(null);

        EventProcessor<? super Event>[] result = fallbackResolver.resolve(unregisteredType);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testResolveWhenParentIsNotNullButMissingFromPoolReturnsEmptyArray() {
        Class<?> unregisteredType = TargetUnregisteredEvent.class;

        Mockito.<Class<?>>when(mockResolver.getRegisteredParentAsExtendsBus(TargetUnregisteredEvent.class)).thenReturn(ResolvedParentEvent.class);

        EventProcessor<? super Event>[] result = fallbackResolver.resolve(unregisteredType);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testResolveWhenParentIsPresentInPoolReturnsPreComputedProcessors() {
        Class<?> unregisteredType = TargetUnregisteredEvent.class;

        @SuppressWarnings("unchecked")
        EventProcessor<? super Event>[] expectedProcessors = new EventProcessor[]{mockProcessor1, mockProcessor2};

        Mockito.<Class<?>>when(mockResolver.getRegisteredParentAsExtendsBus(TargetUnregisteredEvent.class)).thenReturn(ResolvedParentEvent.class);

        preComputedPool.put(ResolvedParentEvent.class, expectedProcessors);

        EventProcessor<? super Event>[] result = fallbackResolver.resolve(unregisteredType);

        assertNotNull(result);
        assertSame(expectedProcessors, result);
        assertEquals(2, result.length);
        assertEquals(mockProcessor1, result[0]);
        assertEquals(mockProcessor2, result[1]);
    }
}