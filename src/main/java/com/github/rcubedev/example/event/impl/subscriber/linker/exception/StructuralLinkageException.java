package com.github.rcubedev.example.event.impl.subscriber.linker.exception;

/**
 * Thrown when access is granted, but the framework cannot mechanically compile 
 * the handler (e.g., LambdaMetafactory constraints or bad MethodType adaptations).
 */
public final class StructuralLinkageException extends Exception {
    public StructuralLinkageException(String message) {
        super(message);
    }

    public StructuralLinkageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StructuralLinkageException(Throwable cause) {
        super(cause);
    }
}