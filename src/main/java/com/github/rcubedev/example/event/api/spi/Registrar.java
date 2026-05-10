package com.github.rcubedev.example.event.api.spi;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
// todo
public interface Registrar<E extends Event> {
    /**
     * Accepts a listener and returns a subscription.<br>
     * The bus implementation will handle the locking and linking.
     *
     * @param type The class of the event to listen for
     * @param priority The priority of this listener
     * @param processor The processor to invoke
     */
    @NotNull Subscription register(Class<E> type, Priority priority, EventProcessor<E> processor);
}