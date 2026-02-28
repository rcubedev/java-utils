package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * Handles registration of @SubscribeEvent methods to event handlers.
 */
public final class EventSubscriberHandler {

    private EventSubscriberHandler() {}

    /**
     * Register all @SubscribeEvent methods from a listener class or instance.
     * 
     * @param handler The event handler to register to
     * @param target A Class (for static methods) or Object instance (for instance methods)
     * @throws IllegalArgumentException if invalid listener or no @SubscribeEvent methods found
     */
    public static <E extends Event> void register(EventHandler<E> handler, Object target) {
        if (target == null) {
            throw new IllegalArgumentException("Cannot register null listener");
        }

        Class<?> type = target.getClass();
        if (type == Method.class) {
            Method method = (Method) target;
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("register() was called with a Method that is not static: " + method);
            }
            if (!method.isAnnotationPresent(SubscribeEvent.class)) {
                throw new IllegalArgumentException("register() was called with a Method that is not annotated with @SubscribeEvent: " + method);
            }
            registerListener(handler, method, method);
            return;
        }

        // Determine if registering a class (static methods) or instance (non-static methods)
        boolean isStatic = type == Class.class;
        Class<?> clazz = isStatic ? (Class<?>) target : type;
        checkSupertypes(clazz, clazz);

        int foundMethods = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) {
                continue;
            }

            if (Modifier.isStatic(method.getModifiers()) == isStatic) {
                registerListener(handler, target, method);
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
                    The event bus only recognizes listener methods that have the @SubscribeEvent annotation.
                    """.formatted(clazz));
        }
    }

    private static void checkSupertypes(Class<?> registeredType, Class<?> type) {
        if (type == null || type == Object.class) {
            return;
        }

        if (type != registeredType) {
            for (var method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubscribeEvent.class)) {
                    throw new IllegalArgumentException("""
                            Attempting to register a listener object of type %s,
                            however its supertype %s has a @SubscribeEvent method: %s.
                            This is not allowed! Only the listener object can have @SubscribeEvent methods.
                            """.formatted(registeredType, type, method));
                }
            }
        }

        checkSupertypes(registeredType, type.getSuperclass());
        Stream.of(type.getInterfaces())
                .forEach(itf -> checkSupertypes(registeredType, itf));
    }

    /**
     * Register a single listener method to the handler.
     */
    private static <E extends Event> void registerListener(EventHandler<E> handler, Object target, Method method) {
        SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
        
        // Validate method signature
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
                    "Method " + method + " has @SubscribeEvent annotation, " +
                            "but takes an argument that is not an Event subtype : " + paramType);
        }

        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("@SubscribeEvent method must return void: " + method); // fixme what if i add something where event can decide w/o using event.setsomething & instead return type
        }

        // Create the event processor
        method.setAccessible(true);
        MethodHandle handle;
        try {
            handle = MethodHandles.publicLookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access @SubscribeEvent method: " + method, e);
        }
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        EventProcessor<E> processor = event -> {
            // Skip cancelled events if configured
            if (annotation.ignoreCancelled() && event instanceof Cancellable cancellable && cancellable.isCancelled()) {
                return;
            }

            try {
                if (isStatic) handle.invoke(event);
                else handle.invoke(target, event);
            } catch (Throwable e) {
                throw new RuntimeException("Error invoking @SubscribeEvent method: " + method, e);
            }
        };

        // Register with the handler
        handler.register(annotation.priority(), processor);
    }
}