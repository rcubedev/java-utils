package com.github.rcubedev.example.event.impl.subscriber.linker.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkageExceptionTests {

    @Test
    void shouldConstructWithReasonAndCause() {
        String expectedMessage = "Failed to generate LambdaMetafactory bootstrapper";
        Throwable expectedCause = new IllegalAccessException("Method handles lookup denied");

        LinkageException exception = new LinkageException(expectedMessage, expectedCause);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedCause, exception.getCause());
    }
}
