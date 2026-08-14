package com.github.rcubedev.utils.registry.api.mutable;

import com.github.rcubedev.utils.registry.api.IdRegistry;
import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;
import com.github.rcubedev.utils.registry.impl.mutable.SimpleMutableKeylessRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A freezable keyless registry of entries of type {@link T}.
 * <p>
 * Entries are added via {@link #register} until {@link #freeze()} is called,
 * after which the registry is immutable and safe to share and reuse freely
 * across as many consumers as needed.
 *
 * @param <T> the type of entry held by this registry
 */
public interface MutableKeylessRegistry<T> extends MutableRegistry<T>, IdRegistry<T> {

    /**
     * Registers an entry.
     *
     * @throws RegistryFrozenException if this registry is frozen
     */
    int register(@NotNull T entry);

    /**
     * Retrieves the auto assigned {@code int} id for a given key.
     *
     * @param id the id to search
     * @return the integer id, or {@link Integer#MIN_VALUE} if not present.
     */
    @NotNull Optional<T> get(int id);

    default @NotNull Optional<T> getById(int id) {
        return get(id);
    }

    static <T> MutableKeylessRegistry<T> create(String name) {
        return new SimpleMutableKeylessRegistry<>(name);
    }
}
