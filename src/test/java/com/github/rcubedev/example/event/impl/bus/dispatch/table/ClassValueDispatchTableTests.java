package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver.Resolver;
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
        try (ClassValueDispatchTable<Event> table = new ClassValueDispatchTable<>(mockResolver, warmUpTypes)) {
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

        try (ClassValueDispatchTable<Event> table = new ClassValueDispatchTable<>(mockResolver, Set.of())) {
            UnregisteredEvent unregisteredEvent = new UnregisteredEvent();
            table.dispatch(unregisteredEvent);

            verify(mockProcessor1, times(1)).process(unregisteredEvent);
        }
        verify(mockResolver, times(1)).resolve(UnregisteredEvent.class);
    }

    @Test
    void testClosePurgesTrackedTypesFromCache() {
        List<EventProcessor<? super Event>> processorsA = List.of(mockProcessor1);
        when(mockResolver.resolve(TestEventA.class)).thenReturn(processorsA);

        ClassValueDispatchTable<Event> table = new ClassValueDispatchTable<>(mockResolver, Set.of(TestEventA.class));

        verify(mockResolver, times(1)).resolve(TestEventA.class);

        table.close();

        // querying the table again after close should re-trigger computation as
        // the old value was evicted.
        TestEventA eventA = new TestEventA();
        table.dispatch(eventA);

        verify(mockResolver, times(2)).resolve(TestEventA.class);
    }
}