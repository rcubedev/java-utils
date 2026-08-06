package com.github.rcubedev.utils.event.impl.subscriber;

import com.github.rcubedev.utils.event.api.*;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.impl.subscriber.linker.LinkerEngine;
import com.github.rcubedev.utils.event.impl.subscriber.linker.MethodLinker;
import com.github.rcubedev.utils.event.impl.subscriber.linker.RuntimeLinkageEngine;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.HandlerInstantiationException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandlerCompiler<B extends Event> {

    // MethodKey.type().parameterType(0) -> Class<T> == HandlerFactory<T> --> generic is the same.
    private static final Map<MethodKey, HandlerFactory<?>> CLASS_METAFACTORIES = new ConcurrentHashMap<>();

    private final LinkerEngine linkageEngine;
    private final MethodValidator<B> validationHook;

    public EventHandlerCompiler(MethodValidator<B> validationHook) {
        this(new RuntimeLinkageEngine(), validationHook);
    }

    // Constructor injection for clean testing later on
    EventHandlerCompiler(LinkerEngine linkageEngine, MethodValidator<B> validationHook) {
        this.linkageEngine = linkageEngine;
        this.validationHook = validationHook;
    }

    //fixme should validate if Identity can register for method if in cache
    public void registerListener(@Nullable Object instance, Method method, Identity identity, Registrar<B> registrar) {
        registerListener0(instance, method, identity, registrar);
    }

    private <E extends B> void registerListener0(@Nullable Object instance, Method method, Identity identity, Registrar<B> registrar) {

        if (!validationHook.isCompatible(method)) {
            validationHook.validate(method); // blow up
            return;
        }
        boolean isStatic = instance == null;

        @SuppressWarnings("unchecked")
        HandlerFactory<E> handlerFactory = (HandlerFactory<E>) CLASS_METAFACTORIES.computeIfAbsent(
                new MethodKey(method), k -> compile(method, identity));

        @SuppressWarnings("unchecked") // safe as passed validation earlier or now (would have thrown if failed)
        Class<E> eventType = (Class<E>) method.getParameterTypes()[0];

        EventProcessor<E> rawProcessor;
        try {
            rawProcessor = isStatic ? handlerFactory.factory().createStatic() : handlerFactory.factory().create(instance);
        } catch (HandlerInstantiationException e) {
            throw new HandlerInstantiationException("Failed to bind listener instance for method: " + method, e);
        }

        EventProcessor<E> processor = handlerFactory.ignoreCancelled() ? event -> {
            if (event instanceof Cancellable c && c.isCancelled()) return;
            rawProcessor.process(event);
        } : rawProcessor;

        registrar.register(eventType, handlerFactory.priority(), processor);
    }

    private HandlerFactory<? extends B> compile(Method method, Identity identity) {
        Class<? extends B> paramType = validationHook.validate(method);
        MethodLinker<? extends B> linker = new MethodLinker<>(method, paramType, identity, linkageEngine);
        return linker.compile();
    }

    /*private <E extends B> HandlerFactory<E> compile(Class<?> targetClass, Class<E> paramType, Method method) {
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

    private <E extends B> HandlerFactory<E> createStrongFactory(Class<?> targetClass, MethodHandle handle, MethodHandles.Lookup lookup, Class<E> paramType, Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup, "process",
                    isStatic ? MethodType.methodType(EventProcessor.class) : MethodType.methodType(EventProcessor.class, targetClass),
                    MethodType.methodType(void.class, Event.class),
                    handle, MethodType.methodType(void.class, paramType)
            );
            MethodHandle factoryHandle = site.getTarget();

            BindingFactory<E> factory;
            if (isStatic) {
                @SuppressWarnings("unchecked")
                EventProcessor<E> processor = (EventProcessor<E>) factoryHandle.invokeExact();
                factory = target -> processor;
            } else {
                factoryHandle = factoryHandle.asType(factoryHandle.type().changeParameterType(0, Object.class));
                MethodHandle finalFactoryHandle = factoryHandle;
                factory = target -> {
                    @SuppressWarnings("unchecked")
                    EventProcessor<E> temp = (EventProcessor<E>) finalFactoryHandle.invokeExact(target);
                    return temp;
                };
            }
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            return new HandlerFactory<>(annotation.priority(), annotation.ignoreCancelled(), factory);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lambda for " + method, e);
        }
    }

    private <E extends B> HandlerFactory<E> createWeakFactory(Class<?> targetClass, MethodHandle handle, MethodHandles.Lookup lookup, Class<E> paramType, Method method) {
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
                    lookup, "process",
                    MethodType.methodType(UnboundProcessor.class),
                    MethodType.methodType(void.class, Object.class, Event.class),
                    handle, MethodType.methodType(void.class, targetClass, paramType)
            );
            MethodHandle factoryHandle = site.getTarget();

            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            @SuppressWarnings("unchecked")
            UnboundProcessor<Object, E> unbound = (UnboundProcessor<Object, E>) factoryHandle.invokeExact();
            return new HandlerFactory<>(annotation.priority(), annotation.ignoreCancelled(), instance -> new WeakEventProcessor<>(instance, unbound));
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lambda for " + method, e);
        }
    }*/
}
