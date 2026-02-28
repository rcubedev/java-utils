package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import java.util.*;

/**
 * Registry that tracks the event type hierarchy for polymorphic dispatching.
 * Allows listeners registered on parent event handlers to receive events from child types.
 */
public final class EventHandlerInheritanceRegistry {

    private static final Map<Class<? extends Event>, List<Class<? extends Event>>> parentCache = new HashMap<>();
    private static final Object lock = new Object();

    private EventHandlerInheritanceRegistry() {}

    /**
     * Get all parent event types for a given event type, in order from most specific to most general.
     * For example, if you have: PlayerLoginEvent extends PlayerEvent extends CancellableEvent extends Event
     * This returns [PlayerLoginEvent, PlayerEvent, CancellableEvent, Event]
     *
     * @param eventType The event type to get parents for
     * @return List of event types from child to parent
     */
    public static List<Class<? extends Event>> getEventHierarchy(Class<? extends Event> eventType) {
        List<Class<? extends Event>> cached = parentCache.get(eventType);
        if (cached != null) {
            return cached;
        }

        synchronized (lock) {
            // Double-check after acquiring lock
            cached = parentCache.get(eventType);
            if (cached != null) {
                return cached;
            }

            List<Class<? extends Event>> hierarchy = new ArrayList<>();
            Class<?> current = eventType;

            while (Event.class.isAssignableFrom(current)) {
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) current;
                hierarchy.add(eventClass);
                current = current.getSuperclass();
            }

            List<Class<? extends Event>> immutable = Collections.unmodifiableList(hierarchy);
            parentCache.put(eventType, immutable);
            return immutable;
        }
    }

    /**
     * Clear the inheritance cache. Useful for testing or if you dynamically modify class hierarchies.
     */
    public static void clearCache() {
        synchronized (lock) {
            parentCache.clear();
        }
    }
}