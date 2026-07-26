package com.github.rcubedev.example.services.api.exception;

/**
 * Thrown by {@link com.github.rcubedev.example.services.api.ServiceRegistry#require(Class)}
 * when no provider is registered for the requested contract.
 */
public final class ServiceNotFoundException extends IllegalStateException {

    private final Class<?> contract;

    public ServiceNotFoundException(Class<?> contract) {
        super("No provider registered for service: " + contract.getName());
        this.contract = contract;
    }

    public Class<?> contract() {
        return contract;
    }
}
