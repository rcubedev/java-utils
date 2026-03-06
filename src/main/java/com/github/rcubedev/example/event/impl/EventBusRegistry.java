package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventBus;

import java.util.Arrays;

/**
 * Global registry of all {@link EventBus} instances.
 * Buses self-register on construction. {@link #dispatch(Event)} fires all registered buses.
 */
public final class EventBusRegistry {

    // Volatile array — writes are rare (bus registration at startup only),
    // reads (dispatch) are frequent and need no locking beyond the volatile read
    private static volatile EventBus<?>[] buses = new EventBus[0];
    private static final Object writeLock = new Object();

    private EventBusRegistry() {}

    /**
     * Register a bus. Called automatically by the {@link EventBus} constructor.
     */
    public static void register(EventBus<?> bus) {
        synchronized (writeLock) {
            EventBus<?>[] current = buses;
            EventBus<?>[] next = Arrays.copyOf(current, current.length + 1);
            next[current.length] = bus;
            buses = next;
        }
    }

    /**
     * Dispatch an event to all registered buses.
     * Each bus checks at runtime whether the event is an instance of its base type.
     * <p>
     * Called automatically by {@link Event#dispatch()}, but also usable directly
     * as the public API for firing all buses at once.
     *
     * @param event The event to dispatch
     */
    public static void dispatch(Event event) {
        EventBus<?>[] snapshot = buses; // single volatile read, no lock needed
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].postUnchecked(event);
        }
    }

    /**
     * For testing — clear all registered buses.
     */
    public static void reset() {
        synchronized (writeLock) {
            buses = new EventBus[0];
        }
    }
}