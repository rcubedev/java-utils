package com.github.rcubedev.utils.event.impl.subscriber.linker.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleAccessExceptionTests {

    @Test
    void shouldConstructWithMessageOnly() {
        String message = "Cannot access module due to strict JPMS boundaries";

        ModuleAccessException exception = new ModuleAccessException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldConstructWithMessageAndCause() {
        String message = "Reflection access denied";
        Throwable cause = new IllegalAccessException("Method is private");

        ModuleAccessException exception = new ModuleAccessException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldConstructWithCauseOnly() {
        Throwable cause = new IllegalAccessException("Module does not open package");

        ModuleAccessException exception = new ModuleAccessException(cause);

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(cause.getMessage()));
        assertEquals(cause, exception.getCause());
    }
}
