package com.github.rcubedev.utils.event.impl.subscriber.compiled;

import com.github.rcubedev.utils.event.api.Cancellable;
import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import com.github.rcubedev.utils.event.generated.EventSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.InstanceSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.StaticSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;
import com.github.rcubedev.utils.test.UnitTestIgnored;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

//todo add method & static binding
public class CompiledSubscriberCompiler {

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

    public <T, B extends Event> List<Subscription> bindStatic0(Class<T> clazz, Identity identity, Registrar<B> registrar) {

        ClassValidator validator = registrar.classValidator();
        validator.validate(clazz);

        @SuppressWarnings("unchecked")
        SubscriberInvokerFactory<T, ?> factory = (SubscriberInvokerFactory<T, ?>) this.factories.get(clazz);
        if (factory == null) return List.of();

        List<Subscription> subscriptions = new ArrayList<>();
        for (EventSubscriberInvoker<T, ?> invokerObj : factory.invokers()) {
            if (!(invokerObj instanceof StaticSubscriberInvoker<T, ?> invoker)) return List.of(); // mismatch
            subscriptions.add(registerStaticInvoker(invoker, registrar));
        }
        return subscriptions;
    }

    public <B extends Event> List<Subscription> bindMethod(Method method, Identity identity, Registrar<B> registrar) {
        return List.of(); //todo
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