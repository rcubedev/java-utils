package com.github.rcubedev.example.event.api;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

public class TestEvent extends Event {

    public TestEvent(@NotNull EventBusRegistry registry) {
        super(registry);
    }

    public TestEvent() {
        super(Mockito.mock(EventBusRegistry.class));
    }

    public static class SubEvent extends TestEvent {}
}