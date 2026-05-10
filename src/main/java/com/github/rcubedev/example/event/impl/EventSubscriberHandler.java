package com.github.rcubedev.example.event.impl;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.github.rcubedev.example.event.api.Cancellable;
import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.SubscribeEvent;
import com.github.rcubedev.example.event.api.Weak;
import com.github.rcubedev.example.event.api.spi.IEventBus;
import com.github.rcubedev.example.event.api.spi.Registrar;

/**
 * Handles registration of {@link SubscribeEvent @SubscribeEvent} methods to a {@link IEventBus}.
 */
public final class EventSubscriberHandler {

    private static final Map<MethodKey, HandlerFactory> CLASS_METAFACTORIES = new ConcurrentHashMap<>();
    private EventSubscriberHandler() {}

    /**
     * Register all {@link SubscribeEvent @SubscribeEvent} methods from a target to the given bus.
     * <p>
     * Must be called in {@code rebuildLock} if applicable.
     *
     * @param bus The bus to register to
     * @param target Instance, {@link Class} (for static methods), or {@link Method}
     * @param registrar The way to register to the bus todo
     * @throws IllegalArgumentException if invalid listener or no {@link SubscribeEvent @SubscribeEvent} methods found
     */
    public static void register(
            IEventBus<? extends Event> bus, Object target, Registrar<? extends Event> registrar) {

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
            registerListener(bus, method.getDeclaringClass(), method, registrar); // fixme shouldnt this be called w/ the method's class? done
            return;
        }

        boolean isStatic = type == Class.class;
        Class<?> clazz = isStatic ? (Class<?>) target : type;

        // todo speed this up by caching verified classes. can't just check the map as individual methods may be registered
        checkSupertypes(clazz, clazz);

        int foundMethods = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;

            if (Modifier.isStatic(method.getModifiers()) == isStatic) {
                registerListener(bus, target, method, registrar);
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
    private static <B extends Event, E extends Event> void registerListener(
            IEventBus<B> bus, Object target, Method method, Registrar<E> registrar) {

        Class<?> paramType = method.getParameterTypes()[0];

        // // Param type must be a subtype of the bus base type
        // // todo is this wanted? a listener may listen for Event and be ignored
        //     commented out for now, check swapped to ensure the param is supertype, subtype or exactly the event bus type
        // if (!bus.getBusType().isAssignableFrom(paramType)) return;
        Class<B> busType = bus.getBusType();
        if (!busType.isAssignableFrom(paramType) && !paramType.isAssignableFrom(busType)) {
            validate(method); // still check if valid instead of just not throwing
            return;
        }
        HandlerFactory handlerFactory = CLASS_METAFACTORIES.computeIfAbsent(new MethodKey(method), methodKey -> EventSubscriberHandler.createFactory(methodKey.clazz(), method));

        boolean isStatic = Modifier.isStatic(method.getModifiers());

        Class<E> eventType = (Class<E>) paramType;
        EventProcessor<E> rawProcessor;
        try {
            rawProcessor = (EventProcessor<E>) handlerFactory.factory().create(target);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lambda for " + method, e);
        }
        EventProcessor<E> processor = handlerFactory.ignoreCancelled() ? event -> {
            if (event instanceof Cancellable c && c.isCancelled()) return;
            rawProcessor.process(event);
        } : rawProcessor;

        // EventProcessor<B> processor = event -> {
        //     if (annotation.ignoreCancelled() && event instanceof Cancellable c && c.isCancelled()) return;
        //     try {
        //         // if (isStatic) handle.invoke(event);
        //         // else handle.invoke(target, event);
        //         // handle.invoke(event);
        //         finalHandle.invokeExact(event);
        //     } catch (Throwable e) {
        //         throw new RuntimeException("Error invoking @SubscribeEvent method: " + method, e);
        //     }
        // };

        registrar.register(eventType, handlerFactory.priority(), processor);
    }

    public static HandlerFactory createFactory(Class<?> targetClass, Method method) {
        Class<?> paramType = validate(method);

        MethodHandles.Lookup lookup;
        try {
            lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access @SubscribeEvent method due to module restrictions: " + method, e);
        }

        MethodHandle handle;
        try {
            handle = lookup.unreflect(method);
        } catch (InaccessibleObjectException | IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access @SubscribeEvent method: " + method, e);
        }

        // todo
        boolean isWeak = method.isAnnotationPresent(Weak.class) || targetClass.isAnnotationPresent(Weak.class);
        return isWeak
                ? createWeakFactory(targetClass, handle, lookup, paramType, method)
                : createStrongFactory(targetClass, handle, lookup, paramType, method);
    }

    @SuppressWarnings("unchecked")
    public static HandlerFactory createWeakFactory(Class<?> targetClass, MethodHandle handle, MethodHandles.Lookup lookup, Class<?> paramType, Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        if (isStatic) { // todo
            throw new IllegalArgumentException("""
                            Expected @SubscribeEvent method %s to NOT be static
                            because it was registered as a weak listener.
                            Either make the method non-static, or remove the @Weak annotation.
                            """.formatted(method));
        }

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    "process",
                    MethodType.methodType(UnboundProcessor.class),
                    MethodType.methodType(void.class, Object.class, Event.class),
                    handle,
                    MethodType.methodType(void.class, targetClass, paramType)
            );
            MethodHandle factoryHandle = site.getTarget();
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            UnboundProcessor<Object, ? extends Event> unbound = (UnboundProcessor<Object, ? extends Event>) factoryHandle.invokeExact();
            return new HandlerFactory(annotation.priority(), annotation.ignoreCancelled(), target -> new WeakEventProcessor<>(target, unbound));
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lambda for " + method, e);
        }
    }

    public static <B extends Event> HandlerFactory createStrongFactory(Class<?> targetClass, MethodHandle handle, MethodHandles.Lookup lookup, Class<?> paramType, Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    "process", // method name in EventProcessor
                    isStatic ? MethodType.methodType(EventProcessor.class) : MethodType.methodType(EventProcessor.class, targetClass),
                    MethodType.methodType(void.class, Event.class), // using void as return for handlers
                    handle,
                    MethodType.methodType(void.class, paramType) // see above
            );
            MethodHandle factoryHandle = site.getTarget();
            Factory factory;
            if (isStatic) {
                @SuppressWarnings("unchecked")
                EventProcessor<B> processor = (EventProcessor<B>) factoryHandle.invokeExact();
                factory = target -> processor;
            } else {
                factoryHandle = factoryHandle.asType(factoryHandle.type().changeParameterType(0, Object.class));
                MethodHandle finalFactoryHandle = factoryHandle;
                factory = target -> (EventProcessor<? extends Event>) finalFactoryHandle.invokeExact(target);
            }
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            return new HandlerFactory(annotation.priority(), annotation.ignoreCancelled(), factory);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lambda for " + method, e);
        }
    }

    private static Class<?> validate(Method method) {
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

        if (method.getReturnType() != void.class) {
            throw new IllegalStateException("@SubscribeEvent method must return void: " + method); // fixme what if i add something where event can decide w/o using event.setsomething & instead return type
        }
        return paramType;
    }

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

    /**
     * Represents a pre-compiled factory for a specific {@link SubscribeEvent @SubscribeEvent} method.
     *
     * @param priority The priority from the annotation
     * @param factory The factory that creates the lambda instance
     */
    public record HandlerFactory(Priority priority, boolean ignoreCancelled, Factory factory) {}

    @FunctionalInterface
    public interface Factory {
        EventProcessor<? extends Event> create(Object target) throws Throwable;
    }
}
