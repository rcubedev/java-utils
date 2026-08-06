package com.github.rcubedev.utils.util.impl;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class Throwables {

    private Throwables() {}

    /**
     * Rethrows the given throwable without requiring it to be declared.
     * <p>
     * This method never returns. The generic return type exists solely so the
     * method can be used in expression contexts, such as {@code return} statements.
     *
     * @param throwable the throwable to rethrow
     * @return never returns
     */
    @Contract("_ -> fail")
    public static <T> T throwUnchecked(@NotNull Throwable throwable) {
        Throwables.<RuntimeException>sneakyThrow(throwable);
        throw new AssertionError("Unreachable");
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }
}
