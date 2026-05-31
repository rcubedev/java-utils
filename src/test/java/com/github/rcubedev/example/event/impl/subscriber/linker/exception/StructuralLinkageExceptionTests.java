package com.github.rcubedev.example.event.impl.subscriber.linker.exception;

import org.junit.jupiter.api.Test;
import java.lang.invoke.WrongMethodTypeException;

import static org.junit.jupiter.api.Assertions.*;

class StructuralLinkageExceptionTests {

    @Test
    void shouldConstructWithMessageOnly() {
        String message = "Structural linkage error message";

        StructuralLinkageException exception = new StructuralLinkageException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldConstructWithMessageAndCause() {
        String message = "MethodType mismatch during LambdaMetafactory bootstrap";
        Throwable cause = new WrongMethodTypeException("Expected (Event)void but found (Object)void");

        StructuralLinkageException exception = new StructuralLinkageException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldConstructWithCauseOnly() {
        Throwable cause = new IllegalArgumentException("Target method is not functional");

        StructuralLinkageException exception = new StructuralLinkageException(cause);

        assertEquals(cause, exception.getCause());
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(cause.getMessage()));
    }
}
