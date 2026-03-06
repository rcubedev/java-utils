package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * Handles registration of {@link SubscribeEvent @SubscribeEvent} methods to an {@link EventBus}.
 */
public final class EventSubscriberHandler {

    private EventSubscriberHandler() {}

    /**
     * Register all {@link SubscribeEvent @SubscribeEvent} methods from a target to the given bus.
     *
     * @param bus       The bus to register to
     * @param target    Instance, {@link Class} (for static methods), or {@link Method}
     * @throws IllegalArgumentException if invalid listener or no {@link SubscribeEvent @SubscribeEvent} methods found
     */
    @SuppressWarnings("unchecked")
    public static <B extends Event> void register(
            EventBus<B> bus, Object target) {

        if (target == null) throw new IllegalArgumentException("Cannot register null listener");

        Class<?> type = target.getClass();

        if (type == Method.class) {
            Method method = (Method) target;
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException(
                        "register() was called with a Method that is not static: " + method);
            }
            if (!method.isAnnotationPresent(SubscribeEvent.class)) {
                throw new IllegalArgumentException(
                        "register() was called with a Method not annotated with @SubscribeEvent: " + method);
            }
            registerListener(bus, method, method);
            return;
        }

        boolean isStatic = type == Class.class;
        Class<?> clazz = isStatic ? (Class<?>) target : type;
        checkSupertypes(clazz, clazz);

        int foundMethods = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;

            if (Modifier.isStatic(method.getModifiers()) == isStatic) {
                registerListener(bus, target, method);
            } else {
                if (isStatic) {
                    throw new IllegalArgumentException("""
                            Expected @SubscribeEvent method %s to be static
                            because register() was called with a class type.
                            Either make the method static, or call register() with an instance of %s.
                            """.formatted(method, clazz));
                } else {
                    throw new IllegalArgumentException("""
                            Expected @SubscribeEvent method %s to NOT be static
                            because register() was called with an instance type.
                            Either make the method non-static, or call register(%s.class).
                            """.formatted(method, clazz.getSimpleName()));
                }
            }
            ++foundMethods;
        }

        if (foundMethods == 0) {
            throw new IllegalArgumentException("""
                    %s has no @SubscribeEvent methods, but register was called anyway.
                    The event bus only recognizes listener methods annotated with @SubscribeEvent.
                    """.formatted(clazz));
        }
    }

    private static void checkSupertypes(Class<?> registeredType, Class<?> type) {
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

    @SuppressWarnings("unchecked")
    private static <B extends Event> void registerListener(
            EventBus<B> bus, Object target, Method method) {

        SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);

        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("@SubscribeEvent method must be public: " + method);
        }
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation. " +
                            "It has " + method.getParameterCount() + " arguments, " +
                            "but event handler methods require a single argument only.");
        }

        Class<?> paramType = method.getParameterTypes()[0];

        if (!Event.class.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent but parameter is not an Event subtype: " + paramType);
        }

        // Param type must be a subtype of the bus base type
        if (!bus.getBusType().isAssignableFrom(paramType)) return;

        if (method.getReturnType() != void.class) {
            throw new IllegalStateException("@SubscribeEvent method must return void: " + method); // fixme what if i add something where event can decide w/o using event.setsomething & instead return type
        }

        boolean accessible = method.trySetAccessible();
        if (!accessible) {
            throw new IllegalStateException(
                    "Cannot access @SubscribeEvent method due to module restrictions: " + method
            );
        }

        MethodHandle handle;
        try {
            handle = MethodHandles.publicLookup().unreflect(method);
        } catch (InaccessibleObjectException | IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access @SubscribeEvent method: " + method, e);
        }
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        Class<B> eventType = (Class<B>) paramType;
        EventProcessor<B> processor = event -> {
            if (annotation.ignoreCancelled() && event instanceof Cancellable c && c.isCancelled()) return;
            try {
                if (isStatic) handle.invoke(event);
                else handle.invoke(target, event);
            } catch (Throwable e) {
                throw new RuntimeException("Error invoking @SubscribeEvent method: " + method, e);
            }
        };

        // Register directly into the bus's registered map. Bypasses the public API
        // to avoid triggering a rebuild per-method (bus.register() will rebuild once after)
        bus.registerDirect(eventType, annotation.priority(), processor);
    }
}