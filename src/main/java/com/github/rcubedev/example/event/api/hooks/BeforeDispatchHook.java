package com.github.rcubedev.example.event.api.hooks;

import com.github.rcubedev.example.event.api.Event;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface BeforeDispatchHook<E extends Event> {

    /**
     * Invoked immediately before an {@link Event} is dispatched to listeners.
     *
     * @param event the event about to be dispatched
     * @implSpec Implementations must not throw.
     */
    void beforeDispatch(@NotNull E event);
}
