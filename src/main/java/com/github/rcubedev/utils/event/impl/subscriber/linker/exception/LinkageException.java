package com.github.rcubedev.utils.event.impl.subscriber.linker.exception;

/**
 * Thrown when the event system cannot compile or link a subscriber method.
 */
public class LinkageException extends RuntimeException {
    public LinkageException(String message, Throwable cause) {
        super(message, cause);
    }
}
