package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A fast thread-safe table that routes events to their handlers.
 * <p>
 * Finding the handlers for an event takes {@code O(1)} constant time.<br>
 * The time to execute dispatch scales linearly, {@code O(H)} based on the number of
 * polymorphic handlers ({@code H}) matched to that specific event type.
 */
public final class DispatchTable<E extends Event> {

    private final ClassValue<EventProcessor<? super E>[]> cache;

    /**
     * Creates a new dispatch table using a pre-sorted map of event handlers.
     *
     * @param preComputedPool a map of ready-to-run handler arrays keyed by event class
     */
    public DispatchTable(Map<Class<? extends E>, EventProcessor<? super E>[]> preComputedPool, FallbackResolver<E> fallback) {
        this.cache = new ClassValue<>() {
            @Override
            protected EventProcessor<? super E>[] computeValue(@NotNull Class<?> type) {
                EventProcessor<? super E>[] processors = preComputedPool.get(type);
                if (processors != null) return processors;
                return fallback.resolve(type);
            }
        };

        // Eager init
        for (Class<? extends E> registeredType : preComputedPool.keySet()) {
            this.cache.get(registeredType);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> @NotNull DispatchTable<T> empty() {
        return (DispatchTable<T>) Holder.EMPTY;
    }

    /**
     * Dispatches the given event to all compatible processors.
     *
     * @param event The event to post.
     */
    public void dispatch(@NotNull E event) {
        EventProcessor<? super E>[] processors = cache.get(event.getClass());
        for (EventProcessor<? super E> processor : processors) processor.process(event);
    }

    private static class Holder {
        @SuppressWarnings("unchecked")
        private static final DispatchTable<?> EMPTY = new DispatchTable<>(Map.of(),
                clazz -> (EventProcessor<? super Event>[]) new EventProcessor<?>[0]);
    }
}
