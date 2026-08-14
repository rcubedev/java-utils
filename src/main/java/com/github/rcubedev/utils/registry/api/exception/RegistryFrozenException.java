package com.github.rcubedev.utils.registry.api.exception;

import com.github.rcubedev.utils.registry.api.mutable.MutableRegistry;

/**
 * Thrown when a mutating operation is attempted on a frozen {@link MutableRegistry MutableRegistry}
 */
public final class RegistryFrozenException extends IllegalStateException {

    public RegistryFrozenException(String registryName) {
        super("Registry '" + registryName + "' is frozen and cannot be modified");
    }
}
