package com.github.rcubedev.utils.event.impl.bus.dispatch.table;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * A fast thread-safe table that routes events to their processors.
 * <p>
 * Finding processors for an event if cached takes {@code O(1)} constant time.<br>
 * The time to execute dispatch scales linearly, {@code O(H)} based on the number of
 * polymorphic processors ({@code H}) matched to that specific event type.
 *
 * @param <E> The base event type bounds for this table
 */
public final class ClassValueDispatchTable<E extends Event> implements DispatchTable<E> {

    private final ClassValue<@Nullable EventProcessor<? super E>[]> cache;
    private final Supplier<? extends DispatchTable<E>> activeTable;
    private final Set<Class<?>> trackedTypes;
    private volatile boolean closed;

    /**
     * Creates a new dispatch table with a resolver for unseen types and an initial
     * set of types to eagerly cache.
     *
     * @param resolver the resolver used to compute processors when encountering an event
     *                 type for the first time
     * @param activeTable a supplier returning the current active table reference for fallback
     *                    routing if this table is closed mid-dispatch
     * @param warmUpTypes a collection of event types to eagerly cache during init
     */
    @SuppressWarnings("unchecked")
    public ClassValueDispatchTable(Resolver<E> resolver, Supplier<? extends DispatchTable<E>> activeTable, Collection<Class<? extends E>> warmUpTypes) {
        this.activeTable = activeTable;
        this.trackedTypes = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

        this.cache = new ClassValue<>() {
            @Override
            protected EventProcessor<? super E>[] computeValue(@NotNull Class<?> type) {
                if (closed) return Holder.empty();
                trackedTypes.add(type);
                return resolver.resolve(type).toArray(EventProcessor[]::new);
            }
        };

        // Eager init for known types
        for (Class<? extends E> registeredType : warmUpTypes) this.cache.get(registeredType);
    }

    /**
     * {@inheritDoc}
     *
     * @param event The event to post.
     */
    public void dispatch(@NotNull E event) {
        EventProcessor<? super E>[] processors = this.cache.get(event.getClass());
        if (this.closed) {
            DispatchTable<E> next = this.activeTable.get();
            if (next == null || next == this) throw new IllegalStateException("Dispatch table is closed activeTable returned invalid target: " + next);
            next.dispatch(event);
            return;
        }
        for (EventProcessor<? super E> processor : Holder.empty()) processor.process(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.closed = true;
        for (Class<?> type : this.trackedTypes) this.cache.remove(type);
        this.trackedTypes.clear();
    }

    private static class Holder {
        private static final EventProcessor<?>[] EMPTY = new EventProcessor<?>[0];

        @SuppressWarnings("unchecked")
        private static <T extends Event> EventProcessor<T>[] empty() {
            return (EventProcessor<T>[]) EMPTY;
        }
    }
}
