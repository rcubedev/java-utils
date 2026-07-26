package com.github.rcubedev.example.event.impl.bus.dispatch.recursion;

import com.github.rcubedev.example.event.api.exceptions.EventStackOverflowException;
import com.github.rcubedev.example.event.api.spi.RecursionBypass;
import org.jetbrains.annotations.NotNull;

public class RecursionGuard {
    // todo(jdk25): scoped values
    private static final ThreadLocal<int[]> depth = ThreadLocal.withInitial(() -> new int[]{0});
    private final int maxStackDepth;

    public RecursionGuard(int maxStackDepth) {
        this.maxStackDepth = maxStackDepth;
    }

    // returns the val before incr.
    public int increment() throws EventStackOverflowException {
        int[] d = depth.get();
        int current = d[0];
        int attempted = current + 1;
        if (attempted > maxStackDepth) throw new EventStackOverflowException("Event recursion too deep (attempted depth: "
                + attempted + ", max stack depth: " + maxStackDepth + "). " +
                "Check for circular posts (e.g. A posts B, B posts A).", attempted, maxStackDepth);
        d[0] = attempted;
        return current;
    }

    public void resetTo(int previousValue) {
        int[] d = depth.get();
        if ((d[0] = previousValue) == 0) depth.remove();
    }

    public @NotNull RecursionBypass bypass(int extraBudget) {
        if (extraBudget < 0) throw new IllegalArgumentException("extraBudget must be positive");

        int[] d = depth.get();
        int original = d[0];
        int newDepth = original - extraBudget;
        d[0] = (newDepth > original) ? Integer.MIN_VALUE : newDepth;

        return () -> d[0] = original;
    }
}
