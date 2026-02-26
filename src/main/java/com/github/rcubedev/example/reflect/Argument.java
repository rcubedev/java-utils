package com.github.rcubedev.example.reflect;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

import com.github.rcubedev.example.reflect.util.Util;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Shorts;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A class representing an argument with a value and a static type.
 * This class ensures that primitive arguments cannot be null and provides
 * a way to create instances of {@link Argument} with various types via {@link Builder} and its nested classes
 */
public final class Argument<T> implements IArgument<T>, IRecordLike {

    private final @Nullable T value;
    private final @NotNull TypedClass<? super T> staticType;
    private final Kind kind;

    private Argument(@Nullable T value, @NotNull TypedClass<? super T> staticType, Kind kind) {
        this.value = value;
        this.staticType = staticType;
        this.kind = kind;
    }

    /**
     * Internal. Use {@link Builder#of(T, TypedClass)} instead.
     * <p>
     * Creates a new {@link Argument} instance with the provided value and type.
     * <p>
     * For var args, use {@link Builder.VarArgs#of} and its overloads.
     *
     * @param value The value of the argument. This can be {@code null} for reference types,
     *              but must not be {@code null} for primitive types.
     * @param type The type of {@code value} at compile time.
     * @return A new {@link Argument} instance with the specified value and type.
     * @throws NullPointerException If the value is {@code null} and the type is primitive; this shouldn't occur.
     */
    @Contract("_, _ -> new")
    @ApiStatus.Internal
    public static <T> @NotNull Argument<T> of(T value, @NotNull TypedClass<? super T> type) {

        Objects.requireNonNull(type, "type must not be null");
        if (type.getTypedClass().isPrimitive()) Objects.requireNonNull(value, "Primitive argument cannot be null");
        return new Argument<>(value, type, Kind.STANDARD);
    }

    /**
     * Internal. Use {@link Builder#of} overloads instead.
     * <p>
     * Creates a new {@link Argument} with the provided value and type.
     * <p>
     * As the type is primitive, the provided value cannot be null. A
     * {@link NullPointerException} will be thrown if the value is null for
     * primitive types.
     *
     * @param value The value of the argument. This can be {@code null} for reference types,
     *              but must not be {@code null} for primitive types.
     * @param type The boxed type of {@code value} at compile time.
     * @return A new {@link Argument} with the specified value and type.
     * @throws NullPointerException If the value is {@code null}.
     * @throws IllegalArgumentException If {@code type} is not primitive
     */
    @Contract("_, _ -> new")
    @ApiStatus.Internal
    public static <T> @NotNull Argument<T> ofPrimitive(@NotNull T value, @NotNull Class<T> type) {
        Objects.requireNonNull(value, "Primitive argument cannot be null");
        Objects.requireNonNull(type, "type must not be null");
        if (!type.isPrimitive()) throw new IllegalArgumentException(type.getName() + " is not a primitive");
        // if (!(Util.wrapPrimitiveOrSame(type).isInstance(value))) throw new IllegalArgumentException(value.getClass().getName() + " does not match " + type.getName());
        return new Argument<>(value, TypedClass.ofPrimitive(type), Kind.STANDARD);
    }

    /**
     * Internal. Use {@link Builder.VarArgs#of(TypedClass, Class, T...)} instead.
     * <p>
     * Creates a new {@link Argument} with the provided values and type.
     *
     * @param type The array type of the var args input at compile time
     * @param clazz The class type of the var args input at compile time
     * @param values The var args of the argument. This can be and contain {@code null}
     * @return A new {@link Argument} with the specified value and type.
     */
    @Contract("_, _, _ -> new")
    @SafeVarargs
    @ApiStatus.Internal
    public static <T> @NotNull Argument<T[]> ofVarArgs(@NotNull TypedClass<? super T[]> type, @NotNull Class<T> clazz, T @Nullable ... values) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        if (values == null) return new Argument<>(null, type, Kind.VAR_ARGS);

        @SuppressWarnings("unchecked")
        T[] safeValues = Arrays.copyOf(values, values.length, (Class<? extends T[]>) Array.newInstance(clazz, values.length).getClass());
        return new Argument<>(safeValues, type, Kind.VAR_ARGS);
    }

    /**
     * Internal. Use {@link Builder.VarArgs#of} overloads instead.
     * <p>
     * Creates a new {@link Argument} with the provided values and type.
     * <p>
     * As the type is primitive, the values cannot contain null. A
     * {@link NullPointerException} will be thrown if the values contain
     * null for primitive types.
     *
     * @param type The primitive array type of the var input at compile time
     * @param values The var args of the argument. This can be {@code null}, but cannot contain {@code null}
     * @return A new {@link Argument} with the specified value and type.
     * @throws NullPointerException If the value is {@code null}.
     * @throws IllegalArgumentException If {@code type} is not a primitive array or {@code value} does not match {@code type}
     */
    @Contract("_, _ -> new")
    @SafeVarargs
    @ApiStatus.Internal
    public static <T, V> @NotNull Argument<T> ofPrimitiveVarArgs(@NotNull TypedClass<T> type, @Nullable V @NotNull ... values) {

        Objects.requireNonNull(type, "type must not be null");

        //noinspection ConstantConditions <-- IntelliJ thinks values is never null as it doesn't understand the annotations correctly
        if (values == null) return new Argument<>(null, type, Kind.VAR_ARGS);

        Class<T> primitiveArrayClass = type.getTypedClass();
        Class<?> primitiveClass = primitiveArrayClass.getComponentType();
        if (primitiveClass == null || !primitiveClass.isPrimitive()) throw new IllegalArgumentException(primitiveArrayClass.getName() + " must be a primitive array");
        Class<?> boxedClass = Util.wrapPrimitive(primitiveClass);
        @SuppressWarnings("unchecked")
        T primSafeValues = (T) Array.newInstance(primitiveClass, values.length);

        for (int i = 0; i < values.length; i++) {
            Object value = Objects.requireNonNull(((Object[]) values)[i], "Primitive array cannot contain null");
            if (!boxedClass.isInstance(value)) throw new IllegalArgumentException(value.getClass().getName() + " does not match " + boxedClass.getName());
            Array.set(primSafeValues, i, value);
        }

        return new Argument<>(primSafeValues, type, Kind.VAR_ARGS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable T get() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull TypedClass<? super T> getStaticType() {
        return staticType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Kind getKind() {
        return kind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component @NotNull [] recordComponents() {
        return new Component[]{
                IRecordLike.of("value", value),
                IRecordLike.of("staticType", staticType),
                IRecordLike.of("kind", kind)
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return recordToString();
    }

    @Override
    public int hashCode() {
        return recordHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // stop intelliJ complaining
        if (obj == null || getClass() != obj.getClass()) return false;
        return recordEquals(obj);
    }

    public enum Kind {
        STANDARD,
        VAR_ARGS
    }

    public static final class Builder {
        private Builder() {}

        /**
         * Creates a new {@link Argument} instance with the provided value and type.
         * <p>
         * For var args, see {@link VarArgs}.
         *
         * @param value the value of the argument. Can be {@code null} if the type is non-primitive.
         * @param type the type of {@code value} at compile time.
         * @param <T> the type of the value.
         * @return a new {@link Argument} instance with the specified value and type.
         */
        public static <T> @NotNull Argument<T> of(T value, @NotNull TypedClass<? super T> type) {
            return Argument.of(value, type);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Integer> of(int value) {
            return primitiveValue(value, int.class);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Long> of(long value) {
            return primitiveValue(value, long.class);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Boolean> of(boolean value) {
            return primitiveValue(value, boolean.class);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Double> of(double value) {
            return primitiveValue(value, double.class);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Float> of(float value) {
            return primitiveValue(value, float.class);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Character> of(char value) {
            return primitiveValue(value, char.class);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Byte> of(byte value) {
            return primitiveValue(value, byte.class);
        }

        /**
         * Creates a new {@link Argument} instance with the provided value.
         *
         * @param value the value of the argument.
         * @return a new {@link Argument} instance with the specified value.
         */
        public static @NotNull Argument<Short> of(short value) {
            return primitiveValue(value, short.class);
        }

        /**
         * Builder for creating varargs {@link Argument} instances.
         * <p>
         * For standard arguments, see {@link Builder}
         */
        public static final class VarArgs {
            private VarArgs() {}

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param type the compile-time varargs type
             * @param clazz the component class of the varargs
             * @param values the values of the argument. Can be or contain {@code null}.
             * @param <T> The component type of the varargs.
             * @return a new {@link Argument} representing the varargs.
             */
            @SafeVarargs
            public static <T> @NotNull Argument<T[]> of(@NotNull TypedClass<? super T[]> type, @NotNull Class<T> clazz, T @Nullable ... values) {
                return Argument.ofVarArgs(type, clazz, values);
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             * Use {@link VarArgs#of(TypedClass, Class, T[])} if TypedClass does not match the varargs type
             *
             * @param type the compile-time varargs type
             * @param values the values for the varargs. Can be or contain {@code null}.
             * @param <T> The component type of the varargs.
             * @return a new {@link Argument} representing the varargs.
             */
            @Contract("_, _ -> new")
            @SafeVarargs
            public static <T> @NotNull Argument<T[]> of(@NotNull TypedClass<T[]> type, T @Nullable ... values) {
                Objects.requireNonNull(type, "type must not be null");

                @SuppressWarnings("unchecked")
                Class<T> arrayType = (Class<T>) type.getTypedClass().getComponentType();
                return ofVarArgs(type, arrayType, values);
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<int[]> of(int... values) {
                return primitiveVarArgs(int[].class, Arrays.stream(values).boxed().toArray(Integer[]::new));
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<long[]> of(long... values) {
                return primitiveVarArgs(long[].class, Arrays.stream(values).boxed().toArray(Long[]::new));
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<boolean[]> of(boolean... values) {
                return primitiveVarArgs(boolean[].class, Booleans.asList(values).toArray(Boolean[]::new));
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<double[]> of(double... values) {
                return primitiveVarArgs(double[].class, Arrays.stream(values).boxed().toArray(Double[]::new));
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<float[]> of(float... values) {
                return primitiveVarArgs(float[].class, Floats.asList(values).toArray(Float[]::new));
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<char[]> of(char... values) {
                return primitiveVarArgs(char[].class, Chars.asList(values).toArray(Character[]::new));
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<byte[]> of(byte... values) {
                return primitiveVarArgs(byte[].class, Bytes.asList(values).toArray(Byte[]::new));
            }

            /**
             * Creates a new varargs {@link Argument} with the provided values.
             *
             * @param values the values for the varargs. Can be {@code null}, but cannot contain null.
             * @return a new {@link Argument} representing the varargs.
             */
            public static @NotNull Argument<short[]> of(short... values) {
                return primitiveVarArgs(short[].class, Shorts.asList(values).toArray(Short[]::new));
            }

            private static <T, B> @NotNull Argument<T> primitiveVarArgs(Class<T> arrayType, B[] boxedValues) {
                return Argument.ofPrimitiveVarArgs(TypedClass.ofPrimitiveArray(arrayType), boxedValues);
            }

            private static <T> @NotNull Argument<T> primitiveVarArgs(Class<T> type, T values) {
                if (!type.isArray()) throw new IllegalArgumentException("type must be array");
                Class<?> boxedComponentType;
                if (type.isPrimitive()) boxedComponentType = Util.wrapPrimitive(type.getComponentType());
                else boxedComponentType = type;

                Object[] boxedValues = (Object[]) Array.newInstance(boxedComponentType, Array.getLength(values));
                Arrays.setAll(boxedValues, i -> Array.get(values, i));
                return primitiveVarArgs(type, boxedValues);
            }
        }

        private static <T> @NotNull Argument<T> primitiveValue(T value, Class<T> clazz) {
            return Argument.ofPrimitive(value, clazz);
        }
    }
}
