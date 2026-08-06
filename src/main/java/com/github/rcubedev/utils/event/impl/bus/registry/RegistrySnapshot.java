package com.github.rcubedev.utils.event.impl.bus.registry;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.impl.bus.handler.ArrayBackedEventSink;
import com.github.rcubedev.utils.event.impl.bus.handler.EventSinkSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class RegistrySnapshot<B extends Event> {

    private final Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers;

    RegistrySnapshot(@NotNull Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers) {
        this.handlers = Collections.unmodifiableMap(handlers);
    }

    public static <B extends Event> RegistrySnapshot<B> create(Map<Class<? extends B>, Map<Priority, ArrayBackedEventSink<? extends B>>> handlers) {
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> snapshot = HashMap.newHashMap(handlers.size());

        for (var entry : handlers.entrySet()) {
            Class<? extends B> k = entry.getKey();
            Map<Priority, ArrayBackedEventSink<? extends B>> v = entry.getValue();
            if (v == null || v.isEmpty()) continue;

            Map<Priority, EventSinkSnapshot<? extends B>> priorityMap = new EnumMap<>(Priority.class);
            for (var sink : v.entrySet()) {
                priorityMap.put(sink.getKey(), sink.getValue().snapshot());
            }
            snapshot.put(k, Collections.unmodifiableMap(priorityMap));
        }
        return new RegistrySnapshot<>(snapshot);
    }

    public static <B extends Event> RegistrySnapshot<B> createFromSnapshots(Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers) {
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> snapshot = HashMap.newHashMap(handlers.size());

        for (var entry : handlers.entrySet()) {
            Class<? extends B> k = entry.getKey();
            Map<Priority, EventSinkSnapshot<? extends B>> v = entry.getValue();
            if (k == null || v == null || v.isEmpty()) continue;
            Map<Priority, EventSinkSnapshot<? extends B>> priorityMap = new EnumMap<>(v);
            snapshot.put(k, Collections.unmodifiableMap(priorityMap));
        }
        return new RegistrySnapshot<>(snapshot);
    }

    public @NotNull @Unmodifiable Map<Class<? extends B>, @Unmodifiable Map<Priority, EventSinkSnapshot<? extends B>>> getHandlers() {
        return handlers;
    }

    @FunctionalInterface
    public interface Factory<B extends Event> {
        RegistrySnapshot<B> create(Map<Class<? extends B>, Map<Priority, ArrayBackedEventSink<? extends B>>> handlers);
    }
}
