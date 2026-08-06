package com.github.rcubedev.utils.event.impl.processor;

import com.github.rcubedev.utils.event.api.Event;

@FunctionalInterface
public interface UnboundProcessor<T, E extends Event> {
    void process(T target, E event);
}
