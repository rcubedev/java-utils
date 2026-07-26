package com.github.rcubedev.example.registry.api.exception;

/**
 * Thrown when a mutating operation is attempted on a frozen
 * {@link com.github.rcubedev.example.registry.api.Registry}.
 */
public final class RegistryFrozenException extends IllegalStateException {

    public RegistryFrozenException(String registryName) {
        super("Registry '" + registryName + "' is frozen and cannot be modified");
    }
}
