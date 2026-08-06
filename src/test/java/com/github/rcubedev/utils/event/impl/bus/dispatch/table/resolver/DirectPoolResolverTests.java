package com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.TestEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DirectPoolResolverTests {

    static class TestEventA extends TestEvent {}
    static class TestEventB extends TestEvent {}

    @Test
    void resolve_ReturnsRegisteredProcessorsWhenTypeMatchesExactly() {
        EventProcessor<Event> processor1 = event -> {};
        EventProcessor<Event> processor2 = event -> {};
        List<EventProcessor<? super Event>> expectedProcessors = List.of(processor1, processor2);

        Map<Class<? extends Event>, List<EventProcessor<? super Event>>> pool = Map.of(TestEventA.class, expectedProcessors);

        DirectPoolResolver<Event> resolver = new DirectPoolResolver<>(pool);

        List<EventProcessor<? super Event>> actualProcessors = resolver.resolve(TestEventA.class);

        assertEquals(expectedProcessors, actualProcessors, 
                "Should return the exact processor list mapped to the type");
    }

    @Test
    void resolve_ReturnsEmptyListWhenTypeIsMissingFromPool() {
        Map<Class<? extends Event>, List<EventProcessor<? super Event>>> pool = Map.of(
            TestEventA.class, List.of(event -> {})
        );

        DirectPoolResolver<Event> resolver = new DirectPoolResolver<>(pool);

        List<EventProcessor<? super Event>> actualProcessors = resolver.resolve(TestEventB.class);

        assertNotNull(actualProcessors, "Should never return null");
        assertTrue(actualProcessors.isEmpty(), "Should return an empty list for missing types");
    }
}