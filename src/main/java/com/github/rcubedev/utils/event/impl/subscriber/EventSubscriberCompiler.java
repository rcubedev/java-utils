package com.github.rcubedev.utils.event.impl.subscriber;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.SubscribeEvent;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import org.jetbrains.annotations.Nullable;

/**
 * Handles registration of {@link SubscribeEvent @SubscribeEvent} methods to a {@link Registrar}.
 */
public final class EventSubscriberCompiler<B extends Event> {

    private final ListenerClassValidator classValidator;
    private final EventHandlerCompiler<B> compiler;

    public EventSubscriberCompiler(Class<B> busType) {
        this(new ListenerClassValidator(), new EventHandlerCompiler<>(new EventMethodValidator<>(busType)));
    }

    EventSubscriberCompiler(ListenerClassValidator classValidator, EventHandlerCompiler<B> compiler) {
        this.classValidator = classValidator;
        this.compiler = compiler;
    }

    /**
     * Register all {@link SubscribeEvent @SubscribeEvent} methods from a target to the given registrar.
     * <p>
     * Must be called in {@code rebuildLock} (if applicable).
     *
     * @param target Instance, {@link Class} (for static methods), or {@link Method}
     * @param registrar The way to register to the bus todo
     * @throws IllegalArgumentException if invalid listener or no {@link SubscribeEvent @SubscribeEvent} methods found
     */
    public void build(Object target, Identity identity, Registrar<B> registrar) {
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
            registerListener(null, method, identity, registrar);
            return;
        }

        boolean isStatic = type == Class.class;
        Class<?> clazz = isStatic ? (Class<?>) target : type;

        // todo speed this up by caching verified classes. can't just check the map as individual methods may be registered
        this.classValidator.validate(clazz);

        int foundMethods = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;

            if (Modifier.isStatic(method.getModifiers()) == isStatic) {
                registerListener(isStatic ? null : target, method, identity, registrar);
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

    //@SuppressWarnings("unchecked")
    private void registerListener(@Nullable Object instance, Method method, Identity identity, Registrar<B> registrar) {
        this.compiler.registerListener(instance, method, identity, registrar);
        /*HandlerFactory handlerFactory = CLASS_METAFACTORIES.computeIfAbsent(new MethodKey(method), methodKey -> createFactory(methodKey.clazz(), method));

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

        registrar.register(eventType, handlerFactory.priority(), processor);*/
    }

    /*public HandlerFactory createFactory(Class<?> targetClass, Method method) {
        Class<?> paramType = this.methodValidator.validate(method);

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
    }*/

    /*
     * Represents a pre-compiled factory for a specific {@link SubscribeEvent @SubscribeEvent} method.
     *
     * @param priority The priority from the annotation
     * @param factory The factory that creates the lambda instance
     */
    /*public record HandlerFactory(Priority priority, boolean ignoreCancelled, Factory factory) {}

    @FunctionalInterface
    public interface Factory {
        EventProcessor<? extends Event> create(Object target) throws Throwable;
    }*/
}
