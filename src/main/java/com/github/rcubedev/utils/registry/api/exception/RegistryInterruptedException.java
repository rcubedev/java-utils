package com.github.rcubedev.utils.registry.api.exception;

public class RegistryInterruptedException extends RuntimeException {
    public RegistryInterruptedException(String registryName, Throwable cause) {
        super("Interrupted while waiting for registry '" + registryName + "' to freeze", cause);
    }
}