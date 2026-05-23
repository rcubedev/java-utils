package com.github.rcubedev.example.event.impl.bus.handler;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;

public class EventSinkSnapshot<E extends Event> {

    private final Class<E> eventType;
    private final Priority priority;
    private final EventProcessor<E> invoker;

    public EventSinkSnapshot(Class<E> eventType, Priority priority, EventProcessor<E> invoker) {
        this.eventType = eventType;
        this.priority = priority;
        this.invoker = invoker;
    }

    public EventProcessor<E> invoker() {
        return invoker;
    }

    public Class<E> eventType() {
        return eventType;
    }

    public Priority priority() {
        return priority;
    }
}
