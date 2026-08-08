package com.github.rcubedev.utils.event.impl.subscriber.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import com.github.rcubedev.utils.event.impl.subscriber.validation.ListenerClassValidator;

/**
 * Scans objects and classes for valid {@link SubscribeEvent @SubscribeEvent} methods
 */
public final class ReflectiveSubscriberScanner {

    public ReflectiveSubscriberScanner() {}

    public DiscoveredMethod scanSingleMethod(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(
                    "register() was called with a Method that is not static: " + method);
        }
        if (!method.isAnnotationPresent(SubscribeEvent.class)) {
            throw new IllegalArgumentException(
                    "register() was called with a Method not annotated with @SubscribeEvent: " + method);
        }
        return new DiscoveredMethod(null, method);
    }

    public List<DiscoveredMethod> scanContainer(Object target, ClassValidator validator) {
        boolean isStatic = target instanceof Class<?>;
        Class<?> clazz = isStatic ? (Class<?>) target : target.getClass();

        // todo speed this up by caching verified classes. can't just check the map as individual methods may be registered
        validator.validate(clazz);

        List<DiscoveredMethod> methods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;

            validateModifier(method, clazz, isStatic);
            methods.add(new DiscoveredMethod(isStatic ? null : target, method));
        }

        if (methods.isEmpty()) {
            throw new IllegalArgumentException("""
                    %s has no @SubscribeEvent methods, but register was called anyway.
                    The event bus only recognizes listener methods annotated with @SubscribeEvent.
                    """.formatted(clazz));
        }
        return methods;
    }

    private void validateModifier(Method method, Class<?> clazz, boolean expectedStatic) {
        if (Modifier.isStatic(method.getModifiers()) == expectedStatic) return;

        if (expectedStatic) {
            throw new IllegalArgumentException("""
                    Expected @SubscribeEvent method %s to be static
                    because register() was called with a class type.
                    Either make the method static, or call register() with an instance of %s.
                    """.formatted(method, clazz));
        }
        throw new IllegalArgumentException("""
                Expected @SubscribeEvent method %s to NOT be static
                because register() was called with an instance type.
                Either make the method non-static, or call register(%s.class).
                """.formatted(method, clazz.getSimpleName()));
    }
}