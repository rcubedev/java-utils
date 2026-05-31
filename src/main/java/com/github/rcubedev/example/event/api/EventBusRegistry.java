package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.api.spi.IEventBus;
import com.github.rcubedev.example.event.impl.EventBusRegistryImpl;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A global registry for {@link IEventBus} instances.
 */
@ApiStatus.NonExtendable
public interface EventBusRegistry {

    /**
     * Gets the global registry instance.
     *
     * @return the singleton registry
     */
    @UnitTestIgnored
    static @NotNull EventBusRegistry getInstance() {
        return EventBusRegistryImpl.Holder.INSTANCE;
    }

    /**
     * Registers a bus to participate in global event dispatching.
     *
     * @param bus The bus to register
     * @param <E> The base event type of the bus
     */
    <E extends Event> void register(@NotNull IEventBus<E> bus);

    /**
     * Dispatch an event to all registered buses.
     * Each bus checks at runtime whether the event is an instance of its base type.
     * <p>
     * Called automatically by {@link Event#dispatch()}, but also usable directly
     * as the public API for firing all public buses at once.
     *
     * @param event The event to dispatch
     */
    <E extends Event> void dispatch(@NotNull E event);
}
