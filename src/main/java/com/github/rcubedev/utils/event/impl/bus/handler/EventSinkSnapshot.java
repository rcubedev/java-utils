package com.github.rcubedev.utils.event.impl.bus.handler;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Priority;

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

    @FunctionalInterface
    public interface Factory<E extends Event> {
        EventSinkSnapshot<E> snapshot(Class<E> eventType, Priority priority, EventProcessor<E> invoker);
    }
}
