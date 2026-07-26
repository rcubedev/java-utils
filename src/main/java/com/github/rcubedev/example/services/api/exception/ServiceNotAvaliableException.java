package com.github.rcubedev.example.services.api.exception;

public class ServiceNotAvaliableException extends IllegalStateException {
    public ServiceNotAvaliableException(String message) {
        super(message);
    }
}
