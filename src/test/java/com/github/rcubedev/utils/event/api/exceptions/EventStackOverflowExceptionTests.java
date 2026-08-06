package com.github.rcubedev.utils.event.api.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventStackOverflowExceptionTests {

    @Test
    void shouldStoreAndReturnConstructorArgumentsCorrectly() {
        String expectedMessage = "Event recursion limit exceeded!";
        int expectedDepth = 51;
        int expectedMaxDepth = 50;

        EventStackOverflowException exception = new EventStackOverflowException(
                expectedMessage,
                expectedDepth,
                expectedMaxDepth
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedDepth, exception.getDepth());
        assertEquals(expectedMaxDepth, exception.getMaxDepth());
    }
}