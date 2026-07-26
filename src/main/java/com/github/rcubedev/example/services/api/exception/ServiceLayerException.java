package com.github.rcubedev.example.services.api.exception;

import com.github.rcubedev.example.services.api.spi.ServiceLayer;

/**
 * Thrown when a {@link ServiceLayer} cannot be constructed
 * or its backing mechanism fails to initialise.
 */
public final class ServiceLayerException extends IllegalStateException {

    public ServiceLayerException(String message) {
        super(message);
    }

    public ServiceLayerException(String message, Throwable cause) {
        super(message, cause);
    }
}
