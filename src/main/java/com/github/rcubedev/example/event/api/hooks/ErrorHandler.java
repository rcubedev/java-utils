package com.github.rcubedev.example.event.api.hooks;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ErrorHandler<B> {

    /**
     * Handles an exception thrown during event dispatch.
     *
     * @param event The event being dispatched
     * @param error the throwable that was caught
     * @throws Throwable the handler can rethrow the error or throw a new one
     * @implNote If the error is an {@link InterruptedException}, the bus will have
     * already restored the thread's interrupted status before calling this handler.
     */
    void handle(@NotNull B event, @NotNull Throwable error) throws Throwable;
}
