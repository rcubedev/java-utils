package com.github.rcubedev.utils.event.impl.subscriber.compiled;

import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class InvokerFactoryRegistry {

    // Class<T> -> SubscriberInvokerFactory<T, ?>
    private final Map<Class<?>, SubscriberInvokerFactory<?, ?>> factories;

    public InvokerFactoryRegistry(Map<Class<?>, SubscriberInvokerFactory<?, ?>> factories) {
        this.factories = Map.copyOf(factories);
    }

    public <T> Optional<SubscriberInvokerFactory<T, ?>> findFactory(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        SubscriberInvokerFactory<T, ?> invokerFactory = (SubscriberInvokerFactory<T, ?>) factories.get(clazz);
        return Optional.ofNullable(invokerFactory);
    }

    public static InvokerFactoryRegistry loadDefault() {

        // todo can use custom services API in future
        @SuppressWarnings("unchecked")
        ServiceLoader<SubscriberInvokerFactory<?, ?>> loader = ServiceLoader.load(
                (Class<SubscriberInvokerFactory<?, ?>>) (Class<?>) SubscriberInvokerFactory.class,
                SubscriberInvokerFactory.class.getClassLoader());

        Map<Class<?>, SubscriberInvokerFactory<?, ?>> map = loader.stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toUnmodifiableMap(SubscriberInvokerFactory::targetClass, Function.identity()));

        return new InvokerFactoryRegistry(map);
    }
}