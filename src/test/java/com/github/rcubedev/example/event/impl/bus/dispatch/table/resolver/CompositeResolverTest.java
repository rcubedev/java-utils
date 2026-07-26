package com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.TestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeResolverTest {

    @Mock
    private Resolver<Event> mockResolver1;

    @Mock
    private Resolver<Event> mockResolver2;

    @Mock
    private Resolver<Event> mockResolver3;

    @Mock
    private EventProcessor<Event> mockProcessor1;

    @Mock
    private EventProcessor<Event> mockProcessor2;

    static class SomeUnregisteredEvent extends TestEvent {}

    @Test
    void testResolveIteratesInOrderAndShortCircuitsOnFirstMatch() {
        Class<?> targetType = SomeUnregisteredEvent.class;
        
        List<EventProcessor<? super Event>> expectedProcessors = List.of(mockProcessor1, mockProcessor2);
        
        when(mockResolver1.resolve(targetType)).thenReturn(List.of());
        when(mockResolver2.resolve(targetType)).thenReturn(expectedProcessors);

        CompositeResolver<Event> composite = new CompositeResolver<>(
                List.of(mockResolver1, mockResolver2, mockResolver3)
        );

        List<EventProcessor<? super Event>> result = composite.resolve(targetType);

        assertNotNull(result);
        assertSame(expectedProcessors, result);
        assertEquals(2, result.size());

        verify(mockResolver1, times(1)).resolve(targetType);
        verify(mockResolver2, times(1)).resolve(targetType);
        verifyNoInteractions(mockResolver3);
    }

    @Test
    void testResolveReturnsEmptyArrayWhenAllResolversAreEmpty() {
        Class<?> targetType = SomeUnregisteredEvent.class;

        when(mockResolver1.resolve(targetType)).thenReturn(List.of());
        when(mockResolver2.resolve(targetType)).thenReturn(List.of());

        CompositeResolver<Event> composite = new CompositeResolver<>(
                List.of(mockResolver1, mockResolver2)
        );

        List<EventProcessor<? super Event>> result = composite.resolve(targetType);

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(mockResolver1, times(1)).resolve(targetType);
        verify(mockResolver2, times(1)).resolve(targetType);
    }

    @Test
    void testResolveWithNoResolversReturnsEmptyArray() {
        Class<?> targetType = SomeUnregisteredEvent.class;

        CompositeResolver<Event> composite = new CompositeResolver<>(List.of());

        List<EventProcessor<? super Event>> result = composite.resolve(targetType);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
