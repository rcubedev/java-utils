package com.github.rcubedev.example.event.impl.subscriber.linker.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandlerInstantiationExceptionTests {

    @Test
    void shouldConstructWithMessageOnly() {
        String expectedMessage = "Failed to invoke constructor for generated event handler";

        HandlerInstantiationException exception = new HandlerInstantiationException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldConstructWithMessageAndCause() {
        String expectedMessage = "Target listener class could not be instantiated";
        Throwable expectedCause = new NoSuchMethodException("No no-arg constructor found");

        HandlerInstantiationException exception = new HandlerInstantiationException(expectedMessage, expectedCause);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedCause, exception.getCause());
    }
}