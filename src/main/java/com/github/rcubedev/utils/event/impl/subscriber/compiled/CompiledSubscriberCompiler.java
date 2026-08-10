package com.github.rcubedev.utils.event.impl.subscriber.compiled;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import com.github.rcubedev.utils.event.api.subscriber.validation.MethodValidator;
import com.github.rcubedev.utils.event.generated.EventSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.InstanceSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.StaticSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;
import com.github.rcubedev.utils.event.impl.subscriber.HandlerFactory;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CompiledSubscriberCompiler {

    private final InvokerFactoryRegistry factoryRegistry;
    private final MethodHandlerCache methodCache;

    @UnitTestIgnored
    public CompiledSubscriberCompiler() {
        this(InvokerFactoryRegistry.loadDefault());
    }

    @UnitTestIgnored
    public CompiledSubscriberCompiler(InvokerFactoryRegistry factoryRegistry) {
        this(factoryRegistry, new MethodHandlerCache(new MethodHandlerResolver(factoryRegistry)));
    }

    CompiledSubscriberCompiler(InvokerFactoryRegistry factoryRegistry, MethodHandlerCache methodCache) {
        this.factoryRegistry = factoryRegistry;
        this.methodCache = methodCache;
    }

    public <B extends Event> List<Subscription> bindInstance(Object target, Identity identity, Registrar<B> registrar) {
        return bindInstance0(target, identity, registrar);
    }

    private <T, B extends Event> List<Subscription> bindInstance0(T target, Identity identity, Registrar<B> registrar) {
        @SuppressWarnings("unchecked")
        Class<T> targetClass = (Class<T>) target.getClass();

        ClassValidator validator = registrar.classValidator();
        validator.validate(targetClass); // don't use fast #isCompatible, allow it to blow up

        SubscriberInvokerFactory<T, ?> factory = factoryRegistry.findFactory(targetClass).orElse(null);
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

        SubscriberInvokerFactory<T, ?> factory = factoryRegistry.findFactory(clazz).orElse(null);
        if (factory == null) return List.of();

        List<Subscription> subscriptions = new ArrayList<>(factory.invokers().size());
        for (EventSubscriberInvoker<T, ?> invokerObj : factory.invokers()) {
            if (!(invokerObj instanceof StaticSubscriberInvoker<T, ?> invoker)) return List.of(); // mismatch
            subscriptions.add(registerStaticInvoker(invoker, registrar));
        }
        return subscriptions;
    }

    public <B extends Event> Optional<Subscription> bindInstanceMethod(Object instance, Method method, Identity identity,
                                                                       Registrar<B> registrar) {
        return Optional.ofNullable(bindMethod0(instance, method, identity, registrar));
    }

    public <B extends Event> Optional<Subscription> bindStaticMethod(Method method, Identity identity, Registrar<B> registrar) {
        return Optional.ofNullable(bindMethod0(null, method, identity, registrar));
    }

    private <T, B extends Event, E extends B> @Nullable Subscription bindMethod0(@Nullable T target, Method method,
                                                                                 Identity identity, Registrar<B> registrar) {
        Class<?> declaringClass = method.getDeclaringClass();
        if (target != null && !declaringClass.isInstance(target)) {
            throw new IllegalArgumentException("Target instance type " + target.getClass().getName() +
                    " does not match declaring class " + declaringClass.getName() + " of method " + method);
        }

        MethodValidator<B> validationHook = registrar.methodValidator();
        Class<? extends B> validatedType = validationHook.validateParameter(method.getParameterTypes()[0]); // blow up

        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) declaringClass;

        @SuppressWarnings("unchecked") // bind E to the method param type; this is safe as it's not returned or a param
        Class<E> eventType = (Class<E>) validatedType;

        HandlerFactory<E> handlerFactory = methodCache.getOrCompute(clazz, method, registrar.classValidator());
        if (handlerFactory == null) return null;

        EventProcessor<E> processor = handlerFactory.factory().create(target);
        return registrar.register(eventType, handlerFactory.priority(), processor);
    }

    private <T, B extends Event, E extends B> Subscription registerInstanceInvoker(T listener,
                                                                                   InstanceSubscriberInvoker<T, ?> invoker,
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
}
