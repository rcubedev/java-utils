package com.github.rcubedev.utils.event.impl;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventBusRegistry;
import com.github.rcubedev.utils.event.api.spi.IEventBus;
import org.jetbrains.annotations.NotNull;

public final class EventBusRegistryImpl implements EventBusRegistry {

    // Volatile array as writes are rare (bus registration at startup only),
    // reads (dispatch) are frequent and need no locking beyond the volatile read
    private volatile IEventBus<?>[] buses = new IEventBus[0];
    private final Object writeLock = new Object();

    EventBusRegistryImpl() {}

    public <E extends Event> void register(@NotNull IEventBus<E> bus) {
        synchronized (writeLock) {
            IEventBus<?>[] current = buses;
            IEventBus<?>[] next = new IEventBus<?>[current.length + 1];
            System.arraycopy(current, 0, next, 0, current.length);
            next[current.length] = bus;
            buses = next;
        }
    }

    public <E extends Event> void dispatch(@NotNull E event) {
        IEventBus<?>[] snapshot = buses; // single volatile read, no lock needed
        for (IEventBus<?> bus : snapshot) {
            if (bus.getBusType().isInstance(event)) {
                @SuppressWarnings("unchecked")
                IEventBus<? super E> castedBus = (IEventBus<? super E>) bus;
                castedBus.post(event);
            }
        }
    }

    public static class Holder {
        public static final EventBusRegistryImpl INSTANCE = new EventBusRegistryImpl();
        private Holder() {}
    }
}