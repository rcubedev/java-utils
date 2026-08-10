package com.github.rcubedev.utils.event.impl.subscriber;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;

/**
 * Represents a pre-compiled factory for a specific {@link SubscribeEvent @SubscribeEvent} method.
 *
 * @param priority The priority from the annotation
 * @param factory The factory that creates the lambda instance
 */
public record HandlerFactory<E extends Event>(Priority priority, boolean ignoreCancelled, BindingFactory<E> factory) {

    public interface Provider {
        <E extends Event> HandlerFactory<E> create(Priority priority, boolean ignoreCancelled, BindingFactory<E> factory);
    }
}
