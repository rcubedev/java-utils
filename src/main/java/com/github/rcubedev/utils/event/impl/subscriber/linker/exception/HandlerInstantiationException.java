package com.github.rcubedev.utils.event.impl.subscriber.linker.exception;

public class HandlerInstantiationException extends RuntimeException {
    
    public HandlerInstantiationException(String message) {
        super(message);
    }

    public HandlerInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}