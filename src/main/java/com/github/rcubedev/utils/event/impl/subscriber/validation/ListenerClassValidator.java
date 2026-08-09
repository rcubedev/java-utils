package com.github.rcubedev.utils.event.impl.subscriber.validation;

import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.stream.Stream;

public class ListenerClassValidator implements ClassValidator {

    @Override
    public void validate(@NotNull Class<?> clazz) throws IllegalArgumentException {
        checkSupertypes(clazz, clazz);
    }

    /*@Override
    public boolean isCompatible(@NotNull Class<?> clazz) {
        return !clazz.isInterface() && !clazz.isPrimitive() && !clazz.isArray() && !clazz.isAnnotation();
    }*/

    private static void checkSupertypes(@NotNull Class<?> registeredType, Class<?> type) throws IllegalArgumentException {
        if (type == null || type == Object.class) return;
        if (type != registeredType) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubscribeEvent.class)) {
                    throw new IllegalArgumentException("""
                            Attempting to register a listener object of type %s,
                            however its supertype %s has a @SubscribeEvent method: %s.
                            This is not allowed! Only the listener object itself can have @SubscribeEvent methods.
                            """.formatted(registeredType, type, method));
                }
            }
        }
        checkSupertypes(registeredType, type.getSuperclass());
        Stream.of(type.getInterfaces()).forEach(itf -> checkSupertypes(registeredType, itf));
    }
}
