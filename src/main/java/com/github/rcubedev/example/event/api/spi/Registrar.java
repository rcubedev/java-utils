package com.github.rcubedev.example.event.api.spi;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
// todo flushing is not handled by registrar directly, but is done after. maybe add it in a method here
public interface Registrar<T extends Event> {
    /**
     * Accepts a listener and returns a subscription.
     * <p>
     * The bus implementation will handle the locking and linking.
     *
     * @param type The class of the event to listen for
     * @param priority The priority of this listener
     * @param processor The processor to invoke
     */
    <E extends T> @NotNull Subscription register(Class<E> type, Priority priority, EventProcessor<E> processor);
}