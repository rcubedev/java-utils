package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.impl.EventHandlerInheritanceRegistry;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class RegisteredParentResolver<B extends Event> {

    private final Class<B> busType;
    private final RegistrySnapshot<B> snapshot;

    public RegisteredParentResolver(Class<B> busType, RegistrySnapshot<B> snapshot) {
        this.busType = busType;
        this.snapshot = snapshot;
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
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers = snapshot.getHandlers();
        Class<? super E>[] hierarchy = EventHandlerInheritanceRegistry.getEventHierarchy(type);
        for (int i = 1; i < hierarchy.length; i++) {
            Class<? super E> ancestor = hierarchy[i];
            if (busType.isAssignableFrom(ancestor) && handlers.containsKey(ancestor)) {
                return ancestor;
            }
        }
        return null;
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
        Class<? super @NotNull E>[] hierarchy = EventHandlerInheritanceRegistry.getEventHierarchy(type);
        return hierarchy.length;
    }
}
