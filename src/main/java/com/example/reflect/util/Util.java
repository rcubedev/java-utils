package com.example.reflect.util;

import java.util.Objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Util {

    private static final BiMap<Class<?>, Class<?>> PRIMITIVE_LOOKUP = ImmutableBiMap.of( // key should have same generic type as value
            int.class, Integer.class,
            long.class, Long.class,
            boolean.class, Boolean.class,
            double.class, Double.class,
            float.class, Float.class,
            char.class, Character.class,
            byte.class, Byte.class,
            short.class, Short.class,
            void.class, Void.class
    );

    /**
     * Converts a primitive class type to its boxed equivalent.
     * Returns {@code null} if {@code clazz} is not a boxed primitive
     *
     * @param clazz the class to wrap
     * @return boxed class equivalent if primitive, else {@code null}
     */
    public static <T> @Nullable Class<T> wrapPrimitiveOrNull(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz cannot be null");
        @SuppressWarnings("unchecked")
        Class<T> wrapped = (Class<T>) PRIMITIVE_LOOKUP.get(clazz);
        return wrapped;
    }

    /**
     * Converts a primitive class type to its boxed equivalent.
     * Throws if it is not primitive
     *
     * @param clazz the class to wrap
     * @return boxed class equivalent if primitive, else the same class
     */
    public static <T> @NotNull Class<T> wrapPrimitive(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz cannot be null");
        Class<T> wrapped = wrapPrimitiveOrNull(clazz);
        if (wrapped == null) throw new IllegalArgumentException(clazz + " is not a primitive.");
        return wrapped;
    }

    /**
     * Converts a primitive class type to its boxed equivalent.
     * Returns the original class if it is not primitive
     *
     * @param clazz the class to wrap
     * @return boxed class equivalent if primitive, else the same class
     */
    public static <T> @NotNull Class<T> wrapPrimitiveOrSame(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz cannot be null");
        Class<T> wrapped = wrapPrimitiveOrNull(clazz);
        if (wrapped == null) return clazz;
        return wrapped;
    }

    /**
     * Converts a boxed class to its primitive equivalent.
     * Returns {@code null} if the primitive equivalent cannot be found.
     *
     * @param clazz the class to unwrap
     * @return primitive class equivalent if known wrapper, else null
     */
    public static <T> @Nullable Class<T> unwrapPrimitiveOrNull(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz cannot be null");
        @SuppressWarnings("unchecked")
        Class<T> unwrapped = (Class<T>) PRIMITIVE_LOOKUP.inverse().get(clazz);
        return unwrapped;
    }

    /**
     * Converts a boxed class to its primitive equivalent.
     * Throws if it cannot find the primitive version
     *
     * @param clazz the class to unwrap
     * @return primitive class equivalent if known wrapper
     */
    public static <T> @NotNull Class<T> unwrapPrimitive(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz cannot be null");
        Class<T> unwrapped = unwrapPrimitiveOrNull(clazz);
        if (unwrapped == null) throw new IllegalArgumentException(clazz + " is not a boxed primitive.");
        return unwrapped;
    }

    /**
     * Converts a boxed class to its primitive equivalent.
     * Returns the original class if it cannot find the primitive version
     *
     * @param clazz the class to unwrap
     * @return primitive class equivalent if known wrapper, else the same class
     */
    public static <T> @NotNull Class<T> unwrapPrimitiveOrSame(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "wrapperClass cannot be null");
        Class<T> unwrapped = unwrapPrimitiveOrNull(clazz);
        if (unwrapped == null) return clazz;
        return unwrapped;
    }
}
