package com.github.rcubedev.example;

import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;
import org.jetbrains.annotations.NotNull;

/**
 * An interface for Enums that need to be serialized as Strings for QuiltConfig
 *
 * @param <T> The {@link Enum} implementing this interface.
 *
 * @implSpec Implementations of this interface must pass the enum type itself as the generic type parameter.
 *           For example, an enum {@code MyEnum} must implement the interface as follows:
 *           <pre>{@code
 *           public enum MyEnum implements SerializableEnum<MyEnum> { ... }
 *           }</pre>
 *
 * @implNote The {@link #getSelf} method provides a default implementation that simply casts the current instance
 *           to the required enum type {@link T}. This is safe as long as the implementing class correctly defines
 *           the generic as itself.
 */
// impls are preferred to be final to prevent potential issues for subclasses. enums are nonextendable, so this is a nonissue and therefore is not mentioned in javadoc
public interface ISerializableEnum<T extends Enum<T> & ISerializableEnum<T>> extends ConfigSerializableObject<String> {

    /**
     * Returns the {@link Class} object corresponding to the enum type {@link T}.
     * <p>
     * This method is used to get the enum's declaring class.
     *
     * @return the {@link Class} object representing the enum type {@link T}.
     * @implNote This method is satisfied by the final implementation in {@link Enum#getDeclaringClass()}.
     */
    Class<T> getDeclaringClass();

    /**
     * Returns the name of this enum constant, exactly as declared in its declaration.
     *
     * @return the name of the enum constant.
     * @implNote This method is satisfied by the final implementation in {@link Enum#name()}.
     */
    String name();

    /**
     * Helper to bridge the current instance to the generic type {@link T}.
     *
     * @return this instance as type {@link T}.
     * @throws ClassCastException if the implementing class mismatched the generic parameter.
     * @implSpec The default implementation performs an unchecked cast to {@link T}.
     *           This is safe as long as the implementing class provides its
     *           own type as the generic parameter
     */
    @SuppressWarnings("unchecked")
    default T getSelf() {
        try {
            return (T) this;
        } catch (ClassCastException cce) {
            throw new ClassCastException();
        }
    }

    // default T[] values() {
    //     return getDeclaringClass().getEnumConstants();
    // }

    /**
     * Converts the provided string representation of the enum constant to the enum value.
     *
     * @param name the name of the enum constant to return.
     * @return the enum constant corresponding to the given name.
     * @throws IllegalArgumentException if the name does not match any of the constants
     */
    default T valueOfImpl(@NotNull String name) {
        return Enum.valueOf(getDeclaringClass(), name);
    }

    @Override
    default T convertFrom(String representation) {
        return valueOfImpl(representation);
    }

    @Override
    default String getRepresentation() {
        return this.name();
    }

    /**
     * Returns a copy of this enum constant.
     * Since enums are immutable, this method simply returns the current instance.
     *
     * @return the current enum constant instance.
     * @implNote This method simply returns this instance as enums are immutable and cannot be modified.
     */
    @Override
    default T copy() {
        return getSelf();
    }
}
