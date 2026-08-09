package com.github.rcubedev.utils.event.api.subscriber.validation;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClassValidator {

    /**
     * Validates that a subscriber {@link Class} hierarchy complies with the rules of the event bus.
     *
     * @param clazz The class to inspect
     * @throws IllegalArgumentException if the class or any of its supertypes violate structural or
     *                                  hierarchy rules
     */
    void validate(@NotNull Class<?> clazz) throws IllegalArgumentException;

    /**
     * Performs a lightweight initial check to determine if a {@link Class} is candidate-compatible.
     *
     * @param clazz the {@link Class} to inspect for basic compatibility
     * @return {@code true} if the method passes initial checks; {@code false} otherwise
     * @apiNote This is a fast preliminary check (e.g. verifying not an interface) and does not
     *          guarantee that full validation via {@link #validate(Class)} will succeed.
     */
    default boolean isCompatible(Class<?> clazz) {
        return !clazz.isInterface();
    }
}
