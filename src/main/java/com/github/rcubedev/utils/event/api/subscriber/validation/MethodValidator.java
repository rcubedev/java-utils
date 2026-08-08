package com.github.rcubedev.utils.event.api.subscriber.validation;

import com.github.rcubedev.utils.event.api.Event;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@FunctionalInterface
public interface MethodValidator<E extends Event> {

    /**
     * Validates that a {@link Method} complies with the structural rules of the event bus.
     *
     * @param method the method to inspect
     * @throws IllegalArgumentException if the method violates structural preconditions
     */
    default Class<? extends E> validate(Method method) throws IllegalArgumentException {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("@SubscribeEvent method must be public: " + method);
        }
        if (method.getParameterCount() != 1) throw new IllegalArgumentException(
                "@SubscribeEvent method must have 1 argument: " + method);
        if (method.getReturnType() != void.class)
            throw new IllegalArgumentException("@SubscribeEvent method must return void: " + method);
        return validateParameter(method.getParameterTypes()[0]);
    }

    /**
     * Validates that a parameter type is assignable to this event bus type.
     *
     * @param paramType the parameter type to inspect
     * @return the typed event class
     * @throws IllegalArgumentException if the class is not assignable
     */
    Class<? extends E> validateParameter(Class<?> paramType);

    /**
     * Performs a lightweight initial check to determine if a {@link Method} is candidate-compatible.
     *
     * @param method the method to inspect for basic compatibility
     * @return {@code true} if the method passes initial structural checks; {@code false} otherwise
     * @apiNote This is a fast preliminary check (e.g. verifying parameter count) and does not
     *          guarantee that full validation via {@link #validate(Method)} will succeed.
     */
    default boolean isCompatible(Method method) {
        return method.getParameterCount() == 1;
    }
}
