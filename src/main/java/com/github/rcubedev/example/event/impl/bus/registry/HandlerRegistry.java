package com.github.rcubedev.example.event.impl.bus.registry;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.bus.handler.ArrayBackedEventSink;
import com.github.rcubedev.example.event.impl.bus.registry.factory.EventSinkFactory;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HandlerRegistry<B extends Event> {

    // Only accessed inside rebuild. Generic types should match; safe to cast ArrayBackedEventHandler<T>
    // if getting from Class<T>
    private final Map<Class<? extends B>, Map<Priority, ArrayBackedEventSink<? extends B>>> handlers = new HashMap<>();
    private final EventSinkFactory<B> sinkFactory;
    private final RegistrySnapshot.Factory<B> snapshotFactory;

    @UnitTestIgnored
    public HandlerRegistry() {
        this(ArrayBackedEventSink::new, RegistrySnapshot::create);
    }

    HandlerRegistry(EventSinkFactory<B> sinkFactory, RegistrySnapshot.Factory<B> snapshotFactory) {
        this.sinkFactory = sinkFactory;
        this.snapshotFactory = snapshotFactory;
    }

    public <E extends B> void add(Class<E> eventType, Priority priority, EventProcessor<E> listener, Subscription subscription) {
        getOrCreateHandler(eventType, priority).addListener(listener, subscription);
    }

    @SuppressWarnings("unchecked")
    private <E extends B> @Nullable Map<Priority, ArrayBackedEventSink<E>> getHandlers(Class<E> type) {
        return (Map<Priority, ArrayBackedEventSink<E>>) (Map<Priority, ?>) this.handlers.get(type);
    }

    public <E extends B> boolean remove(Class<E> eventType, Priority priority, Subscription subscription) {
        Map<Priority, ArrayBackedEventSink<E>> priorityHandlers = getHandlers(eventType);
        boolean removed = false;
        if (priorityHandlers != null) {
            ArrayBackedEventSink<E> handler = priorityHandlers.get(priority);
            if (handler != null) removed = handler.removeListener(subscription);
        }
        return removed;
    }


    private <E extends B> ArrayBackedEventSink<E> getOrCreateHandler(Class<E> eventType, Priority priority) {
        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<E> result = (ArrayBackedEventSink<E>) handlers.computeIfAbsent(eventType,
                        k -> new EnumMap<>(Priority.class))
                .computeIfAbsent(priority, p -> this.sinkFactory.create(eventType, p));
        return result;
    }

    public RegistrySnapshot<B> snapshot() {
        return this.snapshotFactory.create(Collections.unmodifiableMap(this.handlers));
    }
}
