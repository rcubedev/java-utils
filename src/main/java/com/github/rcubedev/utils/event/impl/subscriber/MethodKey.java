package com.github.rcubedev.utils.event.impl.subscriber;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Represents a unique identifier for a method.
 *
 * @param clazz The declaring class of the method
 * @param methodName The method name
 * @param type The {@link MethodType} of the method
 */
public record MethodKey(Class<?> clazz, String methodName, MethodType type) {
    public MethodKey(Method method) {
        this(method.getDeclaringClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()));
    }
}