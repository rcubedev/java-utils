package com.github.rcubedev.example.event.impl.bus.dispatch.recursion;

import com.github.rcubedev.example.event.api.exceptions.EventStackOverflowException;
import com.github.rcubedev.example.event.api.spi.RecursionBypass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RecursionGuardTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecursionGuardTests.class);

    private RecursionGuard guard;
    private final int MAX_DEPTH = 3;

    @BeforeEach
    void setUp() {
        guard = new RecursionGuard(MAX_DEPTH);
        // Ensure every test starts with a clean slate
        cleanupThreadLocal();
    }

    @AfterEach
    void tearDown() {
        // Crucial: Clear the static state so this thread is clean for the next test
        cleanupThreadLocal();
    }

    private void cleanupThreadLocal() {
        try {
            Field field = RecursionGuard.class.getDeclaredField("depth");
            field.setAccessible(true);
            ThreadLocal<?> tl = (ThreadLocal<?>) field.get(null);
            tl.remove();
        } catch (Exception e) {
            // If reflection fails, we fall back to the public reset
            LOGGER.warn("Failed to cleanup recursion guard. Falling back to #resetTo(int). This is more fragile!", e);
            guard.resetTo(0);
        }
    }

    @Test
    void increment_ProtocolAndException() {
        // Normal increments: 0 -> 1, 1 -> 2, 2 -> 3
        assertEquals(0, guard.increment());
        assertEquals(1, guard.increment());
        assertEquals(2, guard.increment());

        // Attempted 4 > Max 3
        assertThrows(EventStackOverflowException.class, guard::increment);
    }

    @Test
    void resetTo_FullCoverage() throws Exception {
        // 1. Test branch: reset to non-zero (within max depth)
        guard.increment(); // 0 -> 1
        guard.resetTo(2);  // Reset to 2
        assertEquals(2, guard.increment(), "Should have reset to 2 and return 2 on next increment");

        // 2. Test branch: reset to zero (the remove() call)
        Field field = RecursionGuard.class.getDeclaredField("depth");
        field.setAccessible(true);
        ThreadLocal<int[]> tl = (ThreadLocal<int[]>) field.get(null);

        int[] originalArray = tl.get();
        guard.resetTo(0); // This triggers depth.remove()

        int[] newArray = tl.get(); // This triggers withInitial()

        assertNotSame(originalArray, newArray, "remove() was not called; the array wasn't purged.");
        assertEquals(0, newArray[0]);
    }

    @Test
    void bypass_ValidatesAndRestores() {
        // Branch: Negative check
        assertThrows(IllegalArgumentException.class, () -> guard.bypass(-1));

        // Branch: Normal logic
        guard.increment(); // depth is 1
        try (RecursionBypass bypass = guard.bypass(2)) {
            // Logic: 1 - 2 = -1
            assertEquals(-1, guard.increment());
        }

        // Back to original
        assertEquals(1, guard.increment());
    }

    @Test
    void bypass_TernaryUnderflow() {
        // Targets: (newDepth > original) ? Integer.MIN_VALUE : newDepth
        // We start at a very low negative to force a wrap-around when subtracting MAX_VALUE
        guard.resetTo(-10);

        try (RecursionBypass bypass = guard.bypass(Integer.MAX_VALUE)) {
            // If wrap-around occurred, it should be clamped to MIN_VALUE
            int current = guard.increment();
            assertTrue(current <= Integer.MIN_VALUE + 1);
        }
    }
}
