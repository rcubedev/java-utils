package com.github.rcubedev.utils.registry.api.mapped;

import com.github.rcubedev.utils.registry.api.IdRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A read only registry mapping entries of type {@link K} to {@link V}
 *
 * @param <V> the entry type
 */
public interface MappedIdRegistry<K, V> extends MappedRegistry<K, V>, IdRegistry<V> {

    /**
     * Retrieves the auto assigned {@code int} id for a given key.
     *
     * @param key the key to search
     * @return the integer id, or {@link Integer#MIN_VALUE} if not present.
     */
    int getId(@NotNull K key);

    @Override
    default @NotNull Optional<V> get(@NotNull K key) {
        return getById(getId(key));
    }
}
