package com.github.rcubedev.utils.event.impl.subscriber.linker.exception;

/**
 * Thrown when the event engine cannot bridge module boundaries or access
 * member visibility (e.g., JPMS strictness, private methods, or illegal access).
 */
public class ModuleAccessException extends Exception {

    public ModuleAccessException(String message) {
        super(message);
    }

    public ModuleAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModuleAccessException(Throwable cause) {
        super(cause);
    }
}
