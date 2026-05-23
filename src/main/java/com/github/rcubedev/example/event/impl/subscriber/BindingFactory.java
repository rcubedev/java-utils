package com.github.rcubedev.example.event.impl.subscriber;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.HandlerInstantiationException;

@FunctionalInterface
public interface BindingFactory<E extends Event> {
    EventProcessor<E> create(Object obj) throws HandlerInstantiationException;

    default EventProcessor<E> createStatic() throws HandlerInstantiationException {
        return create(null);
    }
}
