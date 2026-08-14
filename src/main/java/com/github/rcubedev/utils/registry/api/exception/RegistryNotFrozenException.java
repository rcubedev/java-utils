package com.github.rcubedev.utils.registry.api.exception;

import com.github.rcubedev.utils.registry.api.mutable.mapped.MutableMappedRegistry;

/**
 * Thrown when a get operation is attempted on a frozen
 * {@link MutableMappedRegistry}.
 */
public final class RegistryNotFrozenException extends IllegalStateException {

    public RegistryNotFrozenException(String registryName) {
        super("Registry '" + registryName + "' is frozen and cannot be modified");
    }
}
