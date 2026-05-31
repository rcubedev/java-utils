package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.TestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchTableTest {

    @Mock
    private EventProcessor<Event> mockProcessor1;

    @Mock
    private EventProcessor<Event> mockProcessor2;

    @Mock
    private FallbackResolver<Event> mockFallback;

    static class TestEventA extends TestEvent {}
    static class TestEventB extends TestEvent {}
    static class UnregisteredEvent extends TestEvent {}

    @Test
    @SuppressWarnings("unchecked")
    void testEagerInitializationAndSuccessfulDispatch() {
        Map<Class<? extends Event>, EventProcessor<? super Event>[]> preComputedPool = new HashMap<>();
        EventProcessor<? super Event>[] processorsA = new EventProcessor[]{mockProcessor1};
        EventProcessor<? super Event>[] processorsB = new EventProcessor[]{mockProcessor2};
        
        preComputedPool.put(TestEventA.class, processorsA);
        preComputedPool.put(TestEventB.class, processorsB);

        DispatchTable<Event> table = new DispatchTable<>(preComputedPool, mockFallback);
        
        TestEventA eventA = new TestEventA();
        table.dispatch(eventA);

        verify(mockProcessor1, times(1)).process(eventA);
        verify(mockProcessor2, never()).process(any());
        verifyNoInteractions(mockFallback);
    }

    @Test
    void testDispatchTriggersFallbackWhenTypeIsMissingFromPreComputedPool() {
        Map<Class<? extends Event>, EventProcessor<? super Event>[]> preComputedPool = new HashMap<>();
        @SuppressWarnings("unchecked")
        EventProcessor<? super Event>[] fallbackProcessors = new EventProcessor[]{mockProcessor1};
        
        when(mockFallback.resolve(UnregisteredEvent.class)).thenReturn(fallbackProcessors);

        DispatchTable<Event> table = new DispatchTable<>(preComputedPool, mockFallback);
        UnregisteredEvent unregisteredEvent = new UnregisteredEvent();

        table.dispatch(unregisteredEvent);

        verify(mockFallback, times(1)).resolve(UnregisteredEvent.class);
        verify(mockProcessor1, times(1)).process(unregisteredEvent);
    }

    @Test
    void testEmptyFactoryMethodReturnsValidFunctionalInstance() {
        DispatchTable<Event> emptyTable = DispatchTable.empty();
        DispatchTable<Event> emptyTableSecondCall = DispatchTable.empty();
        
        TestEventA event = new TestEventA();

        assertNotNull(emptyTable);
        assertSame(emptyTable, emptyTableSecondCall);
        
        assertDoesNotThrow(() -> emptyTable.dispatch(event));
    }
}