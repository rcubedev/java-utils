package com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.example.event.api.DeadEvent;
import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.TestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadEventResolverTest {

    @Mock
    private EventProcessor<DeadEvent> mockDeadListener1;

    @Mock
    private EventProcessor<DeadEvent> mockDeadListener2;

    private Map<Class<? extends Event>, List<EventProcessor<? super Event>>> preComputedPool;
    private DeadEventResolver<Event> resolver;

    static class SomeUnregisteredEvent extends TestEvent {}

    @BeforeEach
    void setUp() {
        preComputedPool = new HashMap<>();
        resolver = new DeadEventResolver<>(preComputedPool);
    }

    @Test
    void testResolveWhenTargetIsDeadEventItselfReturnsEmptyArrayToPreventRecursion() {
        List<EventProcessor<? super Event>> result = resolver.resolve(DeadEvent.class);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testResolveWhenNoDeadEventListenersAreRegisteredReturnsEmptyArray() {
        List<EventProcessor<? super Event>> result = resolver.resolve(SomeUnregisteredEvent.class);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testResolveWhenDeadEventListenerArrayIsEmptyReturnsEmptyArray() {
        preComputedPool.put(DeadEvent.class, List.of());

        List<EventProcessor<? super Event>> result = resolver.resolve(SomeUnregisteredEvent.class);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testResolveWrapsAndProxiesEventToAllDeadEventListeners() {
        @SuppressWarnings("unchecked")
        List<EventProcessor<? super Event>> deadListeners = (List<EventProcessor<? super Event>>) (List<?>) List.of(mockDeadListener1, mockDeadListener2);
        preComputedPool.put(DeadEvent.class, deadListeners);

        List<EventProcessor<? super Event>> result = resolver.resolve(SomeUnregisteredEvent.class);

        assertNotNull(result);
        assertEquals(1, result.size());

        SomeUnregisteredEvent originalEvent = new SomeUnregisteredEvent();
        result.getFirst().process(originalEvent);

        ArgumentCaptor<DeadEvent> captor1 = ArgumentCaptor.forClass(DeadEvent.class);
        ArgumentCaptor<DeadEvent> captor2 = ArgumentCaptor.forClass(DeadEvent.class);

        verify(mockDeadListener1, times(1)).process(captor1.capture());
        verify(mockDeadListener2, times(1)).process(captor2.capture());

        assertEquals(originalEvent, captor1.getValue().getEvent());
        assertEquals(originalEvent, captor2.getValue().getEvent());
    }
}