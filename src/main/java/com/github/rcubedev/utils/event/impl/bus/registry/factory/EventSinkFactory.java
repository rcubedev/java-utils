package com.github.rcubedev.utils.event.impl.bus.registry.factory;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.impl.bus.handler.ArrayBackedEventSink;

// this is separate as in future abstract EventSink may be utilised instead
@FunctionalInterface
public interface EventSinkFactory<B extends Event> {
    <E extends B> ArrayBackedEventSink<E> create(Class<E> eventType, Priority priority);
}
