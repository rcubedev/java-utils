package com.github.rcubedev.utils.event.api.spi;

import com.github.rcubedev.utils.event.api.*;
import com.github.rcubedev.utils.event.api.exceptions.EventStackOverflowException;
import org.jetbrains.annotations.NotNull;

// todo facade this w identity owned by the facade
/**
 * Interface for event buses typed to a specific {@link Event} subclass {@link B}.
 *
 * @implSpec Implementations must be thread-safe for both registration and dispatch. This interface
 *           mandates a recursion guard: implementations must track the depth of {@link #post(Event)}
 *           calls on a per-thread basis and throw {@link EventStackOverflowException} if the safety
 *           limit is exceeded.
 *           <p>
 *           Listeners must be invoked in a strictly defined order:
 *           <ol>
 *               <li>By {@link Priority} (from {@link Priority#LOWEST} to {@link Priority#MONITOR}).</li>
 *               <li>Within the same priority, by hierarchy (superclasses before subclasses).</li>
 *           </ol>
 *           Registered {@link EventProcessor} instances may implement {@link SubscriptionAware} to receive
 *           the {@link Subscription} created for their registration.
 *
 * @see EventBusRegistry
 * @param <B> The base event type this bus accepts
 */
public interface IEventBus<B extends Event> {

    /**
     * Post an event to this bus.
     * <p>
     * Listeners are invoked synchronously in order of priority.
     *
     * @param event The event to dispatch
     * @throws EventStackOverflowException if the stack depth exceeds the safety limit of this bus.<br>
     *                                     Use {@link #openBypass()} to handle intentional deep recursion.
     */
    <E extends B> void post(E event) throws EventStackOverflowException;

    /**
     * Opens a scope where the recursion guard is disabled for the current thread.
     * <p>
     * <b>Warning:</b> This handle <b>must</b> be closed (ideally via try-with-resources) to prevent state leakage.<br>
     * Failure to close it will leave the recursion guard disabled for the remainder
     * of the thread's lifecycle, risking unhandled {@link StackOverflowError}s.
     * <p>
     * Example usage:
     * <pre>{@code
     * try (RecursionBypass ignored = bus.openBypass()) {
     *     bus.post(new DeepNestedEvent());
     * }
     * }</pre>
     *
     * @return a handle that restores the guard state when closed
     */
    default @NotNull RecursionBypass openBypass() {
        return openBypassTo(Integer.MAX_VALUE / 2);
    }

    /**
     * Opens a scope that extends the recursion budget for the current thread.
     * <p>
     * <b>Warning:</b> This handle <b>must</b> be closed (ideally via try-with-resources) to prevent state leakage.<br>
     * Failure to close it will leave the recursion guard disabled for the remainder
     * of the thread's lifecycle, risking unhandled StackOverflowErrors.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Grants 50 additional levels of recursion before the guard trips
     * try (RecursionBypass ignored = bus.openBypassTo(50)) {
     *     bus.post(new ComplexFeedbackEvent());
     * }
     * }</pre>
     *
     * @param extraBudget The number of additional recursive calls to allow (must be positive).
     * @return a handle that restores the guard state when closed
     */
    @NotNull RecursionBypass openBypassTo(int extraBudget);

    /**
     * Register a direct {@link EventProcessor} for the given event type at {@link Priority#NORMAL}.
     * <p>
     * <b>Important:</b> Processors should ideally handle their own exceptions.<br>
     * Unless handled by the event bus implementation, an unhandled exception
     * thrown by the processor will stop dispatch of the current event and prevent
     * any remaining listeners from being invoked.
     *
     * @param eventType The class of the event to listen for
     * @param listener  The processor to invoke
     * @param identity  The {@link Identity} of the registering caller
     * @param <E>       The specific event type
     * @return the {@link Subscription} for this handler
     */
    default <E extends B> @NotNull Subscription register(Class<E> eventType, EventProcessor<E> listener,
                                                         Identity identity) {
        return register(eventType, Priority.NORMAL, listener, identity);
    }

    /**
     * Register a direct {@link EventProcessor} for the given event type at a specific priority.
     * <p>
     * <b>Important:</b> Processors should ideally handle their own exceptions.<br>
     * Unless handled by the event bus implementation, an unhandled exception
     * thrown by the processor will stop dispatch of the current event and prevent
     * any remaining listeners from being invoked.
     *
     * @param eventType The class of the event to listen for
     * @param priority  The priority of this listener
     * @param listener  The processor to invoke
     * @param identity  The {@link Identity} of the registering caller
     * @param <E>       The specific event type
     * @return the {@link Subscription} for this handler
     */
    <E extends B> @NotNull Subscription register(Class<E> eventType, Priority priority, EventProcessor<E> listener,
                                                 Identity identity);

    /**
     * Register a direct {@link EventProcessor} for the base bus type {@link B} at a specific priority.
     * <p>
     * <b>Important:</b> Processors should ideally handle their own exceptions.<br><br>
     * Unless handled by the event bus implementation, an unhandled exception
     * thrown by the processor will stop dispatch of the current event and prevent
     * any remaining listeners from being invoked.
     *
     * @param priority The priority of this listener
     * @param listener The processor to invoke
     * @param identity The {@link Identity} of the registering caller
     * @return the {@link Subscription} for this handler
     */
    default @NotNull Subscription register(EventProcessor<B> listener, Priority priority, Identity identity) {
        return register(getBusType(), priority, listener, identity);
    }

    /**
     * Register a direct {@link EventProcessor} for the base bus type {@link B} at {@link Priority#NORMAL}.
     * <p>
     * <b>Important:</b> Processors should ideally handle their own exceptions.<br><br>
     * Unless handled by the event bus implementation, an unhandled exception
     * thrown by the processor will stop dispatch of the current event and prevent
     * any remaining listeners from being invoked.
     *
     * @param listener The processor to invoke
     * @param identity The {@link Identity} of the registering caller
     * @return the {@link Subscription} for this handler
     */
    default @NotNull Subscription register(EventProcessor<B> listener, Identity identity) {
        return register(getBusType(), listener, identity);
    }

    /**
     * Register a listener instance or {@link Class} with {@link SubscribeEvent @SubscribeEvent} methods.<br>
     * Only methods whose parameter type is a subtype of {@link B} will be registered.
     * <p>
     * <b>Important:</b> Processors should ideally handle their own exceptions.<br>
     * Unless handled by the event bus implementation, an unhandled exception
     * thrown by the processor will stop dispatch of the current event and prevent
     * any remaining listeners from being invoked.
     *
     * @param target Listener instance or {@link Class} for static methods
     * @param identity The {@link Identity} of the registering caller
     * @return a {@link Subscription} wrapping all listeners for the {@code target}
     * @throws IllegalArgumentException if no valid {@link SubscribeEvent @SubscribeEvent} methods are found.
     */
    @NotNull Subscription register(Object target, Identity identity);

    /**
     * Get the base event type this bus accepts.
     *
     * @return The base event class
     */
    @NotNull Class<B> getBusType();
}