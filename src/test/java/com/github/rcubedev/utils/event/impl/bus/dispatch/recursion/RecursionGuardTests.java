package com.github.rcubedev.utils.event.impl.bus.dispatch.recursion;

import com.github.rcubedev.utils.event.api.exceptions.EventStackOverflowException;
import com.github.rcubedev.utils.event.api.spi.RecursionBypass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RecursionGuardTests {

    private RecursionGuard guard;

    @BeforeEach
    void setUp() {
        int MAX_DEPTH = 3;
        guard = new RecursionGuard(MAX_DEPTH);
        cleanupThreadLocal();
    }

    @AfterEach
    void tearDown() {
        cleanupThreadLocal();
    }

    // fixme add a wrapper obj around TL instead of using reflection
    private ThreadLocal<int[]> getThreadLocal() {
        try {
            Field field = RecursionGuard.class.getDeclaredField("depth");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ThreadLocal<int[]> tl = (ThreadLocal<int[]>) field.get(null);
            return tl;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to obtain inner ThreadLocal", e);
        }
    }

    private void cleanupThreadLocal() {
        getThreadLocal().remove();
    }

    @Test
    void run_NormalNestingAndException() {
        assertEquals(0, getThreadLocal().get()[0]);
        guard.run(() -> {
            assertEquals(1, getThreadLocal().get()[0]);
            guard.run(() -> {
                assertEquals(2, getThreadLocal().get()[0]);
                guard.run(() -> assertEquals(3, getThreadLocal().get()[0]));
            });
        });

        // >max depth (attempting 4 when max is 3) should throw exception
        EventStackOverflowException ex = assertThrows(EventStackOverflowException.class, () ->
            guard.run(() ->
                guard.run(() ->
                    guard.run(() ->
                            guard.run(() -> {})))));
        assertEquals(4, ex.getDepth());
        assertEquals(3, ex.getMaxDepth());
    }

    @Test
    void bypass_ValidatesArguments() {
        assertThrows(IllegalArgumentException.class, () -> guard.bypass(-1));
    }

    @Test
    void bypass_GrantsExtraBudgetAndRestores() {
        guard.run(() ->
                guard.run(() ->
                        guard.run(() -> {
                            // at max depth open a bypass providing 2 extra levels of budget
                            try (RecursionBypass bypass = guard.bypass(2)) {
                                guard.run(() -> guard.run(() -> {}));
                            }
                        })
                )
        );

        assertThrows(EventStackOverflowException.class, () ->
            guard.run(() ->
                guard.run(() ->
                    guard.run(() ->
                        guard.run(() -> {})))));
    }

    @Test
    void bypass_TernaryUnderflow() {
        // (newDepth > original) ? Integer.MIN_VALUE : newDepth
        // start at neg to force a wrap around when subtracting MAX_VALUE
        getThreadLocal().set(new int[]{-10});

        try (RecursionBypass bypass = guard.bypass(Integer.MAX_VALUE)) {
            // if wrap around occurred should be clamped to MIN_VALUE
            assertEquals(Integer.MIN_VALUE, getThreadLocal().get()[0]);
        }
    }
}