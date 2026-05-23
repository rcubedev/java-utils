package com.github.rcubedev.example.event.impl.bus.registry;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class RegistrySnapshot<B extends Event> {

    private final Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers;

    public RegistrySnapshot(@NotNull Map<@NotNull Class<? extends B>, @NotNull Map<Priority, EventSinkSnapshot<? extends B>>> handlers) {
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> copy = HashMap.newHashMap(handlers.size());
        for (Map.Entry<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> entry : handlers.entrySet()) {
            Class<? extends B> k = entry.getKey();
            Map<Priority, EventSinkSnapshot<? extends B>> v = entry.getValue();
            if (v.isEmpty()) {
                copy.put(k, Map.of());
                continue;
            }
            copy.put(k, Collections.unmodifiableMap(new EnumMap<>(v)));
        }

        this.handlers = Map.copyOf(copy);
    }

    public @NotNull Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> getHandlers() {
        return handlers;
    }
}
