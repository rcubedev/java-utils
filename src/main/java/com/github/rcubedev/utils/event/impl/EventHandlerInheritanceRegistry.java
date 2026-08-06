package com.github.rcubedev.utils.event.impl;

import com.github.rcubedev.utils.event.api.Event;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry that tracks the event type hierarchy for polymorphic dispatching.
 * <p>
 * Allows listeners registered on parent event handlers to receive child events.
 */
public final class EventHandlerInheritanceRegistry {

    private static final ClassValue<Class<? extends Event>[]> cache = new ClassValue<>() {
        @Override
        @SuppressWarnings("unchecked")
        protected Class<? extends Event>[] computeValue(@NotNull Class<?> type) {
            List<Class<? extends Event>> hierarchy = new ArrayList<>();
            Class<?> current = type;
            while (Event.class.isAssignableFrom(current)) {
                Class<? extends Event> eventClass = (Class<? extends Event>) current;
                hierarchy.add(eventClass);
                current = current.getSuperclass();
            }
            return hierarchy.toArray(Class[]::new);
        }
    };

    private EventHandlerInheritanceRegistry() {}

    /**
     * Get all event types in the hierarchy for a given event type,
     * ordered from most specific (the type itself) to most general.
     * <p>
     * e.g. {@code [PlayerLoginEvent, PlayerEvent, Event]}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> Class<? super @NotNull T>[] getEventHierarchy(Class<T> eventType) {
        Class<? extends Event>[] hierarchy = cache.get(eventType);
        return (Class<? super T>[]) hierarchy;
    }
}