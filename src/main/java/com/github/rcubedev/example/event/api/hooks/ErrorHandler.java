package com.github.rcubedev.example.event.api.hooks;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ErrorHandler<B> {

    /**
     * Handles an exception thrown during event dispatch.
     * <p>
     * Event listeners should not throw as it is unsafe.
     *
     * @param event The event being dispatched
     * @param error the throwable that was caught
     * @throws Error the handler should rethrow the error.
     * @throws RuntimeException rethrow {@code error} or wrap.
     */
    void handle(@NotNull B event, @NotNull Throwable error) throws Error, RuntimeException;
}
