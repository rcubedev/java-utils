package com.github.rcubedev.utils.registry.api.mutable.mapped;

import com.github.rcubedev.utils.registry.api.mapped.MappedIdRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A mutable registry that maps keys of type {@link K} to values of type {@link V}.<br>
 * Each registered entry is assigned its own {@code int} id.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface MutableMappedIdRegistry<K, V> extends MutableMappedRegistry<K, V>, MappedIdRegistry<K, V> {

    /**
     * Registers an entry under the provided key and returns its auto assigned integer id.
     *
     * @param key the unique key
     * @param entry the entry to register
     * @return the assigned integer id
     */
    int registerId(@NotNull K key, @NotNull V entry);

    @Override
    default void register(@NotNull K key, @NotNull V entry) {
        registerId(key, entry);
    }

    /**
     * Retrieves a key using its {@code int} id for fast {@code O(1)} lookups.
     *
     * @param id the {@code int} id of the entry
     * @return an {@link Optional} containing the value or empty if not found
     */
    @NotNull Optional<V> getById(int id);

    @Override
    default @NotNull Optional<V> get(@NotNull K key) {
        return getById(getId(key));
    }

    /**
     * Retrieves the auto assigned {@code int} id for a given key.
     *
     * @param key the key to search
     * @return the integer id, or {@link Integer#MIN_VALUE} if not present.
     */
    int getId(@NotNull K key);
}
