package com.github.rcubedev.utils.registry.api;

import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * A freezable registry of entries of type {@link T}.
 * <p>
 * Entries are added via {@link #register} until {@link #freeze()} is called,
 * after which the registry is immutable and safe to share and reuse freely
 * across as many consumers as needed.
 *
 * @param <T> the type of entry held by this registry
 */
public interface Registry<T> {

    /**
     * The name of this registry
     */
    @NotNull String name();

    /**
     * Registers an entry.
     *
     * @throws RegistryFrozenException if this registry is frozen
     */
    void register(@NotNull T entry);

    /**
     * Freezes this registry. No further registration is permitted.
     *
     * @throws RegistryFrozenException if already frozen
     */
    void freeze();

    /**
     * Returns all registered entries in registration order.
     *
     * @throws RegistryFrozenException if not yet frozen
     * @return unmodifiable list; never null
     */
    @NotNull @Unmodifiable List<T> entries();
}