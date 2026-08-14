package com.github.rcubedev.utils.registry.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

/**
 * A read only registry of type {@link V}
 *
 * @param <V> the entry type
 */
public interface IdRegistry<V> extends Registry<V> {

    /**
     * Retrieves a key using its {@code int} id for fast {@code O(1)} lookups.
     *
     * @param id the {@code int} id of the entry
     * @return an {@link Optional} containing the value or empty if not found
     */
    @NotNull Optional<V> getById(int id);
}
