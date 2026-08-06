package com.github.rcubedev.utils.event.impl.bus.dispatch.table;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.impl.EventHandlerInheritanceRegistry;
import com.github.rcubedev.utils.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.utils.event.impl.bus.registry.RegistrySnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegisteredParentResolver<B extends Event> {

    private final Class<B> busType;
    private final RegistrySnapshot<B> snapshot;
    private final Map<Class<? extends B>, Class<? extends B>[]> cache = new ConcurrentHashMap<>();

    public RegisteredParentResolver(Class<B> busType, RegistrySnapshot<B> snapshot) {
        this.busType = busType;
        this.snapshot = snapshot;
    }

    /**
     * Get all event types in the registered hierarchy for a given event type,
     * ordered from most specific (the type itself) to most general.
     * <p>
     * e.g.<br>
     * Hierarchy: {@code [PlayerLoginEvent, PlayerConnectionEvent, PlayerEvent, Event]}.<br>
     * {@code PlayerConnectionEvent} not registered<br>
     * Return: {@code [PlayerLoginEvent, PlayerEvent, Event]}
     */
    @SuppressWarnings("unchecked")
    public <E extends B> Class<? super E>[] getRegisteredEventHierarchy(Class<E> type) {
        return (Class<? super E>[]) cache.computeIfAbsent(type, t -> {
            Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers = snapshot.getHandlers();

            Class<? super B>[] rawHierarchy = (Class<? super B>[]) EventHandlerInheritanceRegistry.getEventHierarchy(t);
            List<Class<? extends B>> registeredLine = new ArrayList<>(rawHierarchy.length);

            for (Class<? super B> ancestor : rawHierarchy) {
                if (busType.isAssignableFrom(ancestor) && handlers.containsKey(ancestor)) {
                    registeredLine.add((Class<? extends B>) ancestor);
                }
            }
            return registeredLine.toArray(Class[]::new);
        });
    }

    /**
     * Find the nearest registered ancestor of {@code type} on this bus.
     * <p>
     * The return generic {@code <? extends B>} is a supertype of {@link E}.
     */
    @SuppressWarnings("unchecked") // safe because getRegisteredParent always returns a subtype of B.
    public <E extends B> @Nullable Class<? extends B> getRegisteredParentAsExtendsBus(Class<E> type) {
        return (Class<? extends B>) getRegisteredParent(type);
    }

    /**
     * Find the nearest registered ancestor of {@code type} on this bus.
     * <p>
     * The return generic {@code <? super E>} is a subtype of {@link B}.
     */
    public <E extends B> @Nullable Class<? super E> getRegisteredParent(Class<E> type) {
        Class<? super E>[] registeredHierarchy = getRegisteredEventHierarchy(type);
        if (registeredHierarchy.length == 0) return null;
        if (registeredHierarchy[0] == type) {
            return registeredHierarchy.length > 1 ? registeredHierarchy[1] : null;
        }

        return registeredHierarchy[0];
    }

    /**
     * Hierarchy depth; used to sort types shallowest (superclass) first.
     * <ol>{@code [PlayerLoginEvent, PlayerEvent, Event]}
     * <li>Event</li>
     * <li>2nd least specific (PlayerEvent)</li>
     * <li>Least specific (PlayerLoginEvent)</li>
     * </ol>
     */
    public <E extends B> int hierarchyDepth(Class<E> type) {
        Class<? super @NotNull E>[] hierarchy = getRegisteredEventHierarchy(type);
        return hierarchy.length;
    }
}