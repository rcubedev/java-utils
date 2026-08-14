package com.github.rcubedev.utils.registry.api.mapped;

import com.github.rcubedev.utils.registry.api.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

/**
 * A read only registry mapping entries of type {@link K} to {@link V}
 *
 * @param <V> the entry type
 */
public interface MappedRegistry<K, V> extends Registry<V> {

    @NotNull @Unmodifiable Map<K, V> entryMap();

    @NotNull Optional<V> get(@NotNull K key);
}
