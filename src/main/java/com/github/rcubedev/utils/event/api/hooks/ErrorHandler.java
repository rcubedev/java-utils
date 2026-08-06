package com.github.rcubedev.utils.event.api.hooks;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.util.impl.Throwables;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ErrorHandler<E extends Event> {

    /**
     * Handles a {@link Throwable} thrown during event dispatch.
     * <p>
     * Event listeners should generally avoid throwing exceptions, as failures
     * during dispatch may affect other listeners.
     * <p>
     * Implementations may log, suppress, wrap, or rethrow the throwable.<br>
     * {@link Error}s should usually be rethrown rather than handled.
     *
     * @param event the event being dispatched
     * @param error the throwable that was caught
     */
    void handle(@NotNull E event, @NotNull Throwable error);

    /**
     * Rethrows the given throwable without requiring it to be declared.
     * <p>
     * This method never returns. The generic return type exists solely so the
     * method can be used in expression contexts, such as {@code return} statements.
     *
     * @param throwable the throwable to rethrow
     * @return never returns
     */
    @Contract("_ -> fail")
    static <T> T throwUnchecked(@NotNull Throwable throwable) {
        return Throwables.throwUnchecked(throwable);
    }
}
