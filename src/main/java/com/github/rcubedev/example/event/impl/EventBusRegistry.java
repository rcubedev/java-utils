package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.spi.IEventBus;

/**
 * Global registry of all {@link IEventBus} instances.
 * <p>
 * {@link #dispatch(Event)} fires all registered buses.
 */
public final class EventBusRegistry {

    // Volatile array as writes are rare (bus registration at startup only),
    // reads (dispatch) are frequent and need no locking beyond the volatile read
    private static volatile IEventBus<?>[] buses = new IEventBus[0];
    private static final Object writeLock = new Object();

    private EventBusRegistry() {}

    /**
     * Register a bus.
     */
    public static void register(IEventBus<?> bus) {
        synchronized (writeLock) {
            IEventBus<?>[] current = buses;
            IEventBus<?>[] next = new IEventBus<?>[current.length + 1];
            System.arraycopy(current, 0, next, 0, current.length);
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
    public static <E extends Event> void dispatch(E event) {
        IEventBus<?>[] snapshot = buses; // single volatile read, no lock needed
        for (IEventBus<?> bus : snapshot) {
            if (bus.getBusType().isInstance(event)) {
                @SuppressWarnings("unchecked")
                IEventBus<? super E> castedBus = (IEventBus<? super E>) bus;
                castedBus.post(event);
            }
        }
    }
}