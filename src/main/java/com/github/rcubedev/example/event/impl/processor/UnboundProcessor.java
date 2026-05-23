package com.github.rcubedev.example.event.impl.processor;

import com.github.rcubedev.example.event.api.Event;

@FunctionalInterface
public interface UnboundProcessor<T, E extends Event> {
    void process(T target, E event);
}
