package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class ArrayBackedEventHandler<E extends Event> extends EventHandler<E> {

    private final Function<EventProcessor<E>[], EventProcessor<E>> invokerFactory;
    private final Class<E> eventType;
    private final Object lock = new Object();
    private EventProcessor<E>[] listeners;
    private final Map<Priority, EventPhaseData<E>> phases = new EnumMap<>(Priority.class);
    private final List<EventPhaseData<E>> sortedPhases = new ArrayList<>();

    private volatile EventProcessor<E> invoker;
    // Parent handlers that should also receive this handler's events
    private final List<ArrayBackedEventHandler<? super E>> parentHandlers = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public ArrayBackedEventHandler(Class<E> eventType, Class<EventProcessor<E>> processorType, Function<EventProcessor<E>[], EventProcessor<E>> invokerFactory) {
        this.eventType = eventType;
        this.invokerFactory = invokerFactory;
        this.listeners = (EventProcessor<E>[]) Array.newInstance(processorType, 0);
        update();
    }

    /**
     * Add a parent handler. Called when this handler is created and parent handlers exist.
     */
    void addParentHandler(ArrayBackedEventHandler<? super E> parentHandler) {
        synchronized (lock) {
            parentHandlers.add(parentHandler);
        }
        update();
    }

    public void update() {
        this.invoker = createCompositeInvoker();
    }

    @SuppressWarnings("unchecked")
    private EventProcessor<E> createCompositeInvoker() {
        // Return an invoker that checks for parents at invoke time
        // This allows parents to be registered after children
        return event -> {
            List<ArrayBackedEventHandler<? super E>> parents;
            synchronized (lock) {
                parents = new ArrayList<>(parentHandlers);
            }

            if (parents.isEmpty()) {
                // No parents, just use the factory invoker
                invokerFactory.apply(listeners).process(event);
            } else {
                // Invoke with priority handling for parents
                invokeWithPriorities(event, parents);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void invokeWithPriorities(E event, List<ArrayBackedEventHandler<? super E>> parents) {
        // Collect all listeners from this handler and parents, grouped by priority
        Map<Priority, List<EventProcessor<?>>> listenersByPriority = new EnumMap<>(Priority.class);

        // Add this handler's listeners
        addListenersByPriority(listenersByPriority);

        // Add parent handlers' listeners
        for (ArrayBackedEventHandler<? super E> parent : parents) {
            parent.addListenersByPriority(listenersByPriority);
        }

        // Invoke in priority order
        for (Priority priority : Priority.values()) {
            List<EventProcessor<?>> listeners = listenersByPriority.get(priority);
            if (listeners != null) {
                for (EventProcessor<?> listener : listeners) {
                    ((EventProcessor<E>) listener).process(event);
                }
            }
        }
    }

    /**
     * Add this handler's listeners to the provided map, grouped by priority.
     * Used by child handlers for priority-aware event dispatching.
     */
    void addListenersByPriority(Map<Priority, List<EventProcessor<?>>> map) {
        synchronized (lock) {
            for (EventPhaseData<E> phase : sortedPhases) {
                List<EventProcessor<?>> list = map.computeIfAbsent(phase.priority, k -> new ArrayList<>());
                for (EventProcessor<E> listener : phase.listeners) {
                    list.add(listener);
                }
            }
        }
    }

    @Override
    public void register(@NotNull EventProcessor<E> listener) {
        register(Priority.NORMAL, listener);
    }

    @Override
    public void register(@NotNull Priority priority, @NotNull EventProcessor<E> listener) {
        Objects.requireNonNull(priority, "Tried to register a listener for a null priority!");
        Objects.requireNonNull(listener, "Tried to register a null listener!");

        synchronized (lock) {
            getOrCreatePhase(priority, true).addListener(listener);
            rebuildInvoker(listeners.length + 1);
        }
    }

    @SuppressWarnings("unchecked")
    private EventPhaseData<E> getOrCreatePhase(Priority id, boolean sortIfCreate) {
        EventPhaseData<E> phase = phases.get(id);

        if (phase == null) {
            phase = new EventPhaseData<>(id, (Class<EventProcessor<E>>) listeners.getClass().getComponentType());
            phases.put(id, phase);
            sortedPhases.add(phase);

            if (sortIfCreate) {
                sortedPhases.sort(Comparator.comparing(data -> data.priority));
            }
        }

        return phase;
    }

    private void rebuildInvoker(int newLength) {
        // Rebuild handlers.
        if (sortedPhases.size() == 1) {
            // Special case with a single phase: use the array of the phase directly.
            listeners = sortedPhases.getFirst().listeners;
        } else {
            @SuppressWarnings("unchecked")
            EventProcessor<E>[] newHandlers = (EventProcessor<E>[]) Array.newInstance(listeners.getClass().getComponentType(), newLength);
            int newHandlersIndex = 0;

            for (EventPhaseData<E> existingPhase : sortedPhases) {
                int length = existingPhase.listeners.length;
                System.arraycopy(existingPhase.listeners, 0, newHandlers, newHandlersIndex, length);
                newHandlersIndex += length;
            }

            listeners = newHandlers;
        }

        // Rebuild invoker.
        update();
    }

    @Override
    public EventProcessor<E> invoker() {
        return invoker;
    }

    /**
     * Get the event type this handler manages.
     */
    public Class<E> getEventType() {
        return eventType;
    }

    /**
     * Get all parent handlers registered with this handler.
     */
    public List<ArrayBackedEventHandler<? super E>> getParentHandlers() {
        synchronized (lock) {
            return new ArrayList<>(parentHandlers);
        }
    }
}