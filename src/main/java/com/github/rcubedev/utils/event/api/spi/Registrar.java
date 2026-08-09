package com.github.rcubedev.utils.event.api.spi;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import com.github.rcubedev.utils.event.api.subscriber.validation.MethodValidator;
import com.github.rcubedev.utils.event.impl.subscriber.validation.EventMethodValidator;
import org.jetbrains.annotations.NotNull;

/**
 * Low-level registration callback interface provided by an {@link IEventBus}.
 * <p>
 * Binds raw event processor callbacks to the bus pipeline and exposes validation
 * rules enforced by the bus implementation.
 *
 * @param <T> the base event type supported by this registrar
 */
// todo flushing is not handled by registrar directly, but is done after. maybe add it in a method here
public interface Registrar<T extends Event> {

    /**
     * Get the base event type this registrar accepts.
     *
     * @return The base event class
     */
    @NotNull Class<T> baseType();

    /**
     * Accepts a listener and returns a subscription.
     * <p>
     * The bus implementation will handle the locking and linking.
     *
     * @param type The class of the event to listen for
     * @param priority The priority of this listener
     * @param processor The processor to invoke
     */
    <E extends T> @NotNull Subscription register(Class<E> type, Priority priority, EventProcessor<E> processor);

    /**
     * Provides the {@link ClassValidator} enforced by this registrar.
     * <p>
     * Defaults to a no-op validator that accepts all candidate classes.
     *
     * @return the class validator instance
     */
    default @NotNull ClassValidator classValidator() {
        return c -> {};
    }

    /**
     * Provides the {@link MethodValidator} enforced by this registrar.
     * <p>
     * Defaults to a standard method validator configured for this registrar's {@link #baseType()}.
     *
     * @return the method validator instance
     */
    default @NotNull MethodValidator<T> methodValidator() {
        return new EventMethodValidator<>(baseType());
    }
}
