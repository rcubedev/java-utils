package com.github.rcubedev.utils.registry.api.mutable.mapped;

import com.github.rcubedev.utils.registry.api.mapped.MappedRegistry;
import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;
import com.github.rcubedev.utils.registry.api.mutable.MutableRegistry;
import org.jetbrains.annotations.NotNull;

public interface MutableMappedRegistry<K, V> extends MappedRegistry<K, V>, MutableRegistry<V> {

    /**
     * Registers an entry.
     *
     * @throws RegistryFrozenException if this registry is frozen
     */
    void register(@NotNull K key, @NotNull V entry);
}
