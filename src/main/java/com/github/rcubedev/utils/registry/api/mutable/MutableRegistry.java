package com.github.rcubedev.utils.registry.api.mutable;

import com.github.rcubedev.utils.registry.api.Registry;
import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;

/**
 * Common base interface for all mutable registries.
 *
 * @param <T> the entry type
 */
public interface MutableRegistry<T> extends Registry<T> {

    /**
     * Freezes this registry. No further registration is permitted.
     *
     * @throws RegistryFrozenException if already frozen
     */
    void freeze();
}
