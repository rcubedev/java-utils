package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that tracks the event type hierarchy for polymorphic dispatching.
 * <p>
 * Allows listeners registered on parent event handlers to receive child events.
 */
public final class EventHandlerInheritanceRegistry {

    private static final Map<Class<? extends Event>, Class<? extends Event>[]> cache = new ConcurrentHashMap<>();

    private EventHandlerInheritanceRegistry() {}

    /**
     * Get all event types in the hierarchy for a given event type,
     * ordered from most specific (the type itself) to most general.
     * <p>
     * e.g. {@code [PlayerLoginEvent, PlayerEvent, Event]}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> Class<? super @NotNull T>[] getEventHierarchy(Class<T> eventType) {
        return (Class<? super T>[]) cache.computeIfAbsent(eventType, type -> {
            List<Class<? extends Event>> hierarchy = new ArrayList<>();
            Class<?> current = type;
            while (Event.class.isAssignableFrom(current)) {
                Class<? extends Event> eventClass = (Class<? extends Event>) current;
                hierarchy.add(eventClass);
                current = current.getSuperclass();
            }
            return hierarchy.toArray(Class[]::new);
        });
    }
}