package com.github.rcubedev.utils.event.impl.subscriber.linker.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemberAccessExceptionTests {

    @Test
    void shouldConstructWithCauseOnly() {
        Throwable cause = new IllegalAccessException("Field is not accessible");

        MemberAccessException exception = new MemberAccessException(cause);

        assertEquals(cause, exception.getCause());
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(cause.getMessage()));
    }
}
