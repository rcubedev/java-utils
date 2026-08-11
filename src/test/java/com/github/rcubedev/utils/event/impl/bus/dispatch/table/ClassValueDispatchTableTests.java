package com.github.rcubedev.utils.event.impl.bus.dispatch.table;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.TestEvent;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassValueDispatchTableTests {

    @Mock
    private EventProcessor<Event> mockProcessor1;

    @Mock
    private EventProcessor<Event> mockProcessor2;

    @Mock
    private Resolver<Event> mockResolver;

    @Mock
    private DispatchTable<Event> mockActiveTable;

    static class TestEventA extends TestEvent {}
    static class TestEventB extends TestEvent {}
    static class UnregisteredEvent extends TestEvent {}

    @Test
    void testEagerInitializationAndSuccessfulDispatch() {
        List<EventProcessor<? super Event>> processorsA = List.of(mockProcessor1);
        List<EventProcessor<? super Event>> processorsB = List.of(mockProcessor2);

        when(mockResolver.resolve(TestEventA.class)).thenReturn(processorsA);
        when(mockResolver.resolve(TestEventB.class)).thenReturn(processorsB);

        Set<Class<? extends Event>> warmUpTypes = Set.of(TestEventA.class, TestEventB.class);
        try (ClassValueDispatchTable<Event> table = new ClassValueDispatchTable<>(mockResolver, () -> mockActiveTable, warmUpTypes)) {
            TestEventA eventA = new TestEventA();
            table.dispatch(eventA);

            verify(mockProcessor1, times(1)).process(eventA);
            verify(mockProcessor2, never()).process(any());
        }

        verify(mockResolver, times(1)).resolve(TestEventA.class);
        verify(mockResolver, times(1)).resolve(TestEventB.class);
    }

    @Test
    void testDispatchTriggersResolutionWhenTypeIsMissingFromWarmUpTypes() {
        List<EventProcessor<? super Event>> fallbackProcessors = List.of(mockProcessor1);

        when(mockResolver.resolve(UnregisteredEvent.class)).thenReturn(fallbackProcessors);

        try (ClassValueDispatchTable<Event> table = new ClassValueDispatchTable<>(mockResolver, () -> mockActiveTable, Set.of())) {
            UnregisteredEvent unregisteredEvent = new UnregisteredEvent();
            table.dispatch(unregisteredEvent);

            verify(mockProcessor1, times(1)).process(unregisteredEvent);
        }
        verify(mockResolver, times(1)).resolve(UnregisteredEvent.class);
    }

    @Test
    void testCloseDelegatesToActiveTableAndPreventsReResolution() {
        List<EventProcessor<? super Event>> processorsA = List.of(mockProcessor1);
        when(mockResolver.resolve(TestEventA.class)).thenReturn(processorsA);

        ClassValueDispatchTable<Event> table = new ClassValueDispatchTable<>(mockResolver, () -> mockActiveTable, Set.of(TestEventA.class));

        verify(mockResolver, times(1)).resolve(TestEventA.class);

        table.close();

        // dispatching after close must delegate directly to activeTable and NOT reinvoke mockResolver
        TestEventA eventA = new TestEventA();
        table.dispatch(eventA);

        verify(mockActiveTable, times(1)).dispatch(eventA);
        verify(mockResolver, times(1)).resolve(TestEventA.class);
    }
}