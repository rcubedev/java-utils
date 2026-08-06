package com.github.rcubedev.example.event.api.hooks;

import com.github.rcubedev.example.event.api.Event;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface AfterDispatchHook<E extends Event> {

    /**
     * Invoked immediately after an {@link Event} is dispatched to listeners.
     * <p>
     * This hook is invoked even when dispatch fails.
     *
     * @param event the event about to be dispatched
     * @implSpec Implementations must not throw.
     */
    void afterDispatch(@NotNull E event);
}
