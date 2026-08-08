package com.github.rcubedev.utils.event.api.subscriber;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.impl.subscriber.CompiledSubscriberBinderImpl;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A global binder for compiled event listener targets.
 */
@ApiStatus.NonExtendable
public interface CompiledSubscriberBinder {

    /**
     * Gets the global compiled handler registry instance.
     *
     * @return the singleton registry
     */
    @UnitTestIgnored
    static @NotNull CompiledSubscriberBinder getInstance() {
        return CompiledSubscriberBinderImpl.Holder.INSTANCE;
    }

    /**
     * Attempts to register all compiled handlers for the given target instance
     * using the {@link Registrar} callback.
     *
     * @param target the target listener instance
     * @param identity the identity of the registering caller
     * @param registrar the registration callback provided by the bus
     * @param <B> the base event type of the bus
     * @return the registered {@link Subscription} if a pre-compiled handler set was found and executed,
     *         else empty.
     */
    <B extends Event> List<Subscription> register(@NotNull Object target, @NotNull Identity identity,
                                                  @NotNull Registrar<B> registrar);
}
