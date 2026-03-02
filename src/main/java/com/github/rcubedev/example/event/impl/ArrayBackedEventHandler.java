package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventHandler;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
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
    @SuppressWarnings("unchecked")
    private EventPhaseData<E>[] sortedPhases = new EventPhaseData[0];
    // Parents to merge listeners from, set at handler creation, never changed.
    // Parent handlers should always be created before the child; superclasses are loaded before subclasses
    @SuppressWarnings("unchecked")
    private ArrayBackedEventHandler<? super E>[] parentHandlers = new ArrayBackedEventHandler[0];

    // Children to notify when this handler's listeners change
    @SuppressWarnings("unchecked")
    private ArrayBackedEventHandler<? extends E>[] childHandlers = new ArrayBackedEventHandler[0];
    private volatile EventProcessor<E> invoker;

    @SuppressWarnings("unchecked")
    public ArrayBackedEventHandler(Class<E> eventType, Class<EventProcessor<E>> processorType, Function<EventProcessor<E>[], EventProcessor<E>> invokerFactory) {
        this.eventType = eventType;
        this.invokerFactory = invokerFactory;
        this.listeners = (EventProcessor<E>[]) Array.newInstance(processorType, 0);
        update();
    }

    /**
     * Add a parent handler. Called once at handler creation time.
     */
    void addParentHandler(ArrayBackedEventHandler<? super E> parentHandler) {
        synchronized (lock) {
            parentHandlers = Arrays.copyOf(parentHandlers, parentHandlers.length + 1);
            parentHandlers[parentHandlers.length - 1] = parentHandler;
            parentHandler.addChildHandler(this);
        }
        update();
    }

    /**
     * Add a child handler. Called by addParentHandler. Children are notified when this handler changes.
     */
    private void addChildHandler(ArrayBackedEventHandler<? extends E> childHandler) {
        synchronized (lock) {
            childHandlers = Arrays.copyOf(childHandlers, childHandlers.length + 1);
            childHandlers[childHandlers.length - 1] = childHandler;
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
            getOrCreatePhase(priority).addListener(listener);
        }
        update();
    }

    @Override
    public void register(@NotNull Object target) {
        EventSubscriberHandler.register(this, target);
    }

    @SuppressWarnings("unchecked")
    private EventPhaseData<E> getOrCreatePhase(Priority id) {
        EventPhaseData<E> phase = phases.get(id);

        if (phase == null) {
            phase = new EventPhaseData<>(id, (Class<EventProcessor<E>>) listeners.getClass().getComponentType());
            phases.put(id, phase);
            sortedPhases = Arrays.copyOf(sortedPhases, sortedPhases.length + 1);
            sortedPhases[sortedPhases.length - 1] = phase;
            Arrays.sort(sortedPhases, Comparator.comparing(data -> data.priority));
        }

        return phase;
    }

    /**
     * Rebuild the merged flat listeners array from own phases + all parent phases, in priority order.
     * Then notify children to do the same.
     */
    private void rebuildInvoker() {
        ArrayBackedEventHandler<? super E>[] parents;
        ArrayBackedEventHandler<? extends E>[] children;

        synchronized (lock) {
            parents = parentHandlers;
            children = childHandlers;
        }

        if (parents.length == 0) {
            // No parents — just use own sortedPhases directly
            listeners = buildFlatArray(new EventPhaseData[][]{sortedPhases});
        } else {
            // Merge own sortedPhases with all parent sortedPhases in priority order
            EventPhaseData<?>[][] allPhases = new EventPhaseData[1 + parents.length][];
            allPhases[0] = sortedPhases;
            for (int i = 0; i < parents.length; i++) {
                allPhases[i + 1] = parents[i].sortedPhases;
            }
            listeners = buildFlatArray(allPhases);
        }

        invoker = invokerFactory.apply(listeners);

        // Notify children to rebuild their merged arrays too
        for (ArrayBackedEventHandler<? extends E> child : children) {
            child.rebuildInvoker();
        }
    }

    /**
     * Merge multiple sorted phase arrays into a single flat listener array, in priority order.
     */
    @SuppressWarnings("unchecked")
    private EventProcessor<E>[] buildFlatArray(EventPhaseData<?>[][] allPhases) {
        // Use a temporary EnumMap to merge by priority across all phase arrays
        Map<Priority, List<EventProcessor<?>>> merged = new EnumMap<>(Priority.class);

        for (EventPhaseData<?>[] phases : allPhases) {
            for (EventPhaseData<?> phase : phases) {
                List<EventProcessor<?>> list = merged.computeIfAbsent(phase.priority, k -> new ArrayList<>());
                Collections.addAll(list, phase.listeners);
            }
        }

        // Count total
        int total = merged.values().stream().mapToInt(List::size).sum();
        EventProcessor<E>[] result = (EventProcessor<E>[]) Array.newInstance(
                listeners.getClass().getComponentType(), total);

        int i = 0;
        for (Priority priority : Priority.values()) {
            List<EventProcessor<?>> list = merged.get(priority);
            if (list != null) {
                for (EventProcessor<?> p : list) {
                    result[i++] = (EventProcessor<E>) p;
                }
            }
        }

        return result;
    }

    public void update() {
        rebuildInvoker();
    }

    @Override
    public EventProcessor<E> invoker() {
        return invoker;
    }

    @Override
    public Class<E> getEventType() {
        return eventType;
    }

    public List<ArrayBackedEventHandler<? super E>> getParentHandlers() {
        synchronized (lock) {
            return new ArrayList<>(Arrays.asList(parentHandlers));
        }
    }

    @SuppressWarnings("unchecked")
    void clearListeners() {
        synchronized (lock) {
            phases.clear();
            sortedPhases = new EventPhaseData[0];
            listeners = (EventProcessor<E>[]) Array.newInstance(
                    listeners.getClass().getComponentType(), 0);
            // parentHandlers and childHandlers intentionally NOT cleared; structural, not listener data
        }
        rebuildInvoker();
    }
}