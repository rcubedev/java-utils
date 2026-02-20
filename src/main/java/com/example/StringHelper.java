package com.example;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class StringHelper {

    @Contract("null -> !null")
    public static @Nullable String toString(Object value) {
        if (isStringable(value)) {
            return String.valueOf(value);
        }
        return null;
    }

    @Contract("null -> null")
    public static @Nullable String toStringExcludingNull(Object value) {
        if (isStringableExcludingNull(value)) {
            return String.valueOf(value);
        }
        return null;
    }

    @Contract("null -> true")
    public static boolean isStringable(@Nullable Object value) {
        return value == null || value instanceof String || Validate.isPrimitiveWrapper(value);
    }

    @Contract("null -> false")
    public static boolean isStringableExcludingNull(@Nullable Object value) {
        return value instanceof String || Validate.isPrimitiveWrapper(value);
    }
}
