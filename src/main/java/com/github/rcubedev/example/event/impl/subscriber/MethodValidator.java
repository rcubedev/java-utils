package com.github.rcubedev.example.event.impl.subscriber;

import com.github.rcubedev.example.event.api.Event;
import java.lang.reflect.Method;

@FunctionalInterface
public interface MethodValidator<B extends Event> {
    /**
     * Validates that a method complies with the structural rules of the event bus.
     * @param method The method to inspect
     * @throws IllegalArgumentException if the method violates structural preconditions
     */
    <E extends B> Class<E> validate(Method method) throws IllegalArgumentException;

    default boolean isCompatible(Method method) {
        return method.getParameterCount() == 1;
    }
}