package com.github.rcubedev.utils.event.impl.subscriber.compiled;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.descriptor.method.MethodDescriptor;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import com.github.rcubedev.utils.event.api.subscriber.validation.MethodValidator;
import com.github.rcubedev.utils.event.generated.EventSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.InstanceSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.StaticSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;
import com.github.rcubedev.utils.event.impl.descriptor.method.ReflectionMethodDescriptor;
import com.github.rcubedev.utils.event.impl.subscriber.HandlerFactory;
import com.github.rcubedev.utils.event.impl.subscriber.MethodKey;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

//todo add method & static binding
public final class CompiledSubscriberCompiler {

    // MethodKey.type().parameterType(0) -> Class<T> == Optional<HandlerFactory<T>> --> generic is the same.
    private final Map<MethodKey, Optional<HandlerFactory<?>>> METHOD_CACHE = new ConcurrentHashMap<>();

    // Class<T> -> SubscriberInvokerFactory<T, ?>
    private final Map<Class<?>, SubscriberInvokerFactory<?, ?>> factories;

    @UnitTestIgnored
    public CompiledSubscriberCompiler() {
        this(loadFactories());
    }

    public CompiledSubscriberCompiler(Map<Class<?>, SubscriberInvokerFactory<?, ?>> factories) {
        this.factories = factories;
    }

    public <B extends Event> List<Subscription> bindInstance(Object target, Identity identity, Registrar<B> registrar) {
        return bindInstance0(target, identity, registrar);
    }

    private <T, B extends Event> List<Subscription> bindInstance0(T target, Identity identity, Registrar<B> registrar) {
        @SuppressWarnings("unchecked")
        Class<T> targetClass = (Class<T>) target.getClass();

        ClassValidator validator = registrar.classValidator();
        validator.validate(targetClass); // don't use fast #isCompatible, allow it to blow up

        @SuppressWarnings("unchecked")
        SubscriberInvokerFactory<T, ?> factory = (SubscriberInvokerFactory<T, ?>) this.factories.get(targetClass);
        if (factory == null) return List.of();

        List<Subscription> subscriptions = new ArrayList<>(factory.invokers().size());
        for (EventSubscriberInvoker<T, ?> invokerObj : factory.invokers()) {
            if (!(invokerObj instanceof InstanceSubscriberInvoker<T, ?> invoker)) return List.of(); // mismatch
            subscriptions.add(registerInstanceInvoker(target, invoker, registrar));
        }
        return subscriptions;
    }

    public <B extends Event> List<Subscription> bindStatic(Class<?> clazz, Identity identity, Registrar<B> registrar) {
        return bindStatic0(clazz, identity, registrar);
    }

    private <T, B extends Event> List<Subscription> bindStatic0(Class<T> clazz, Identity identity, Registrar<B> registrar) {

        ClassValidator validator = registrar.classValidator();
        validator.validate(clazz);

        @SuppressWarnings("unchecked")
        SubscriberInvokerFactory<T, ?> factory = (SubscriberInvokerFactory<T, ?>) this.factories.get(clazz);
        if (factory == null) return List.of();

        List<Subscription> subscriptions = new ArrayList<>(factory.invokers().size());
        for (EventSubscriberInvoker<T, ?> invokerObj : factory.invokers()) {
            if (!(invokerObj instanceof StaticSubscriberInvoker<T, ?> invoker)) return List.of(); // mismatch
            subscriptions.add(registerStaticInvoker(invoker, registrar));
        }
        return subscriptions;
    }

    public <B extends Event> Optional<Subscription> bindInstanceMethod(Object instance, Method method, Identity identity, Registrar<B> registrar) {
        return Optional.ofNullable(bindMethod0(instance, method, identity, registrar));
    }

    public <B extends Event> Optional<Subscription> bindStaticMethod(Method method, Identity identity, Registrar<B> registrar) {
        return Optional.ofNullable(bindMethod0(null, method, identity, registrar));
    }

    private <T, B extends Event, E extends B> @Nullable Subscription bindMethod0(@Nullable T target, Method method, Identity identity, Registrar<B> registrar) {
        Class<?> declaringClass = method.getDeclaringClass();
        if (target != null && !declaringClass.isInstance(target))
            throw new IllegalArgumentException("Target instance type " + target.getClass().getName() +
                    " does not match declaring class " + declaringClass.getName() + " of method " + method);

        MethodValidator<B> validationHook = registrar.methodValidator();
        Class<? extends B> validatedType = validationHook.validateParameter(method.getParameterTypes()[0]); // blow up

        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) declaringClass;

        @SuppressWarnings("unchecked")
        Class<E> eventType = (Class<E>) validatedType; // bind E to the method param type

        @SuppressWarnings("unchecked")
        HandlerFactory<E> handlerFactory = (HandlerFactory<E>) METHOD_CACHE.computeIfAbsent(new MethodKey(method),
                k -> Optional.ofNullable(findAndCache(clazz, method, registrar.classValidator())))
                .orElse(null);
        if (handlerFactory == null) return null;

        EventProcessor<E> processor = handlerFactory.factory().create(target);
        return registrar.register(eventType, handlerFactory.priority(), processor);
    }

    private <T, E extends Event> @Nullable HandlerFactory<?> findAndCache(Class<T> clazz, Method method, ClassValidator validator) {
        MethodDescriptor targetDescriptor = ReflectionMethodDescriptor.of(method);

        @SuppressWarnings("unchecked")
        SubscriberInvokerFactory<T, ?> factory = (SubscriberInvokerFactory<T, ?>) this.factories.get(clazz);
        if (factory == null) return null;

        validator.validate(clazz);

        for (EventSubscriberInvoker<T, ?> invokerObj : factory.invokers()) {
            if (!invokerObj.descriptor().equals(targetDescriptor)) continue;

            @SuppressWarnings("unchecked")
            EventSubscriberInvoker<T, E> typedInvoker = (EventSubscriberInvoker<T, E>) invokerObj;
            return createHandlerFactory(method, typedInvoker);
        }
        return null;
    }

    private <T, E extends Event> HandlerFactory<E> createHandlerFactory(Method method, EventSubscriberInvoker<T, E> typedInvoker) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        return switch (typedInvoker) {
            case InstanceSubscriberInvoker<T, E> invoker -> {
                if (isStatic)
                    throw new IllegalStateException("Method " + method + " is static, but matched instance invoker " + invoker.getClass().getName());
                yield new HandlerFactory<>(invoker.priority(), invoker.ignoreCancelled(), obj -> {
                    @SuppressWarnings("unchecked")
                    T typedObj = (T) obj;
                    return invoker.create(typedObj);
                });
            }
            case StaticSubscriberInvoker<T, E> invoker -> {
                if (!isStatic)
                    throw new IllegalStateException("Method " + method + " is non-static, but matched static invoker " + invoker.getClass().getName());
                yield new HandlerFactory<>(invoker.priority(), invoker.ignoreCancelled(), obj -> invoker.create());
            }
        };
    }

    private <T, B extends Event, E extends B> Subscription registerInstanceInvoker(T listener, InstanceSubscriberInvoker<T, ?> invoker,
                                                                                   Registrar<B> registrar) {

        Class<? extends B> validatedType = registrar.methodValidator().validateParameter(invoker.eventType());

        @SuppressWarnings("unchecked")
        Class<E> eventType = (Class<E>) validatedType;

        @SuppressWarnings("unchecked")
        InstanceSubscriberInvoker<T, E> typedInvoker = (InstanceSubscriberInvoker<T, E>) invoker;

        EventProcessor<E> processor = typedInvoker.create(listener);
        return registrar.register(eventType, typedInvoker.priority(), processor);
    }

    private <T, B extends Event, E extends B> Subscription registerStaticInvoker(StaticSubscriberInvoker<T, ?> invoker,
                                                                                 Registrar<B> registrar) {

        Class<? extends B> validatedType = registrar.methodValidator().validateParameter(invoker.eventType());

        @SuppressWarnings("unchecked")
        Class<E> eventType = (Class<E>) validatedType;

        @SuppressWarnings("unchecked")
        StaticSubscriberInvoker<T, E> typedInvoker = (StaticSubscriberInvoker<T, E>) invoker;

        EventProcessor<E> processor = typedInvoker.create();
        return registrar.register(eventType, typedInvoker.priority(), processor);
    }

    private static Map<Class<?>, SubscriberInvokerFactory<?, ?>> loadFactories() {

        // todo can use custom services API in future
        @SuppressWarnings("unchecked")
        ServiceLoader<SubscriberInvokerFactory<?, ?>> loader = ServiceLoader.load(
                (Class<SubscriberInvokerFactory<?, ?>>) (Class<?>) SubscriberInvokerFactory.class,
                SubscriberInvokerFactory.class.getClassLoader());

        return loader.stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toUnmodifiableMap(SubscriberInvokerFactory::targetClass, Function.identity()));
    }
}