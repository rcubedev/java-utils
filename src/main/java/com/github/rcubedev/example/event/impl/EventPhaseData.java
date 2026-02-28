package com.github.rcubedev.example.event.impl;

import java.lang.reflect.Array;
import java.util.Arrays;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;

/**
 * Data of an {@link ArrayBackedEventHandler} phase.
 */
class EventPhaseData<E extends Event> {
    final Priority priority;
    EventProcessor<E>[] listeners;

    @SuppressWarnings("unchecked")
    EventPhaseData(Priority priority, Class<EventProcessor<E>> listenerClass) {
        this.priority = priority;
        this.listeners = (EventProcessor<E>[]) Array.newInstance(listenerClass, 0);
    }

    void addListener(EventProcessor<E> listener) {
        int oldLength = listeners.length;
        listeners = Arrays.copyOf(listeners, oldLength + 1);
        listeners[oldLength] = listener;
    }
}