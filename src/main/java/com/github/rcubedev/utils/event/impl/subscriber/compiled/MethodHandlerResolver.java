package com.github.rcubedev.utils.event.impl.subscriber.compiled;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.descriptor.method.MethodDescriptor;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import com.github.rcubedev.utils.event.generated.EventSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.InstanceSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.StaticSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;
import com.github.rcubedev.utils.event.impl.descriptor.method.ReflectionMethodDescriptor;
import com.github.rcubedev.utils.event.impl.subscriber.HandlerFactory;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class MethodHandlerResolver {

    private final InvokerFactoryRegistry factoryRegistry;
    private final HandlerFactory.Provider handlerFactoryProvider;

    @UnitTestIgnored
    public MethodHandlerResolver(InvokerFactoryRegistry factoryRegistry) {
        this(factoryRegistry, HandlerFactory::new);
    }

    MethodHandlerResolver(InvokerFactoryRegistry factoryRegistry, HandlerFactory.Provider handlerFactoryProvider) {
        this.factoryRegistry = factoryRegistry;
        this.handlerFactoryProvider = handlerFactoryProvider;
    }

    public <T, E extends Event> @Nullable HandlerFactory<?> resolve(Class<T> clazz, Method method, ClassValidator validator) {
        SubscriberInvokerFactory<T, ?> factory = factoryRegistry.findFactory(clazz).orElse(null);
        if (factory == null) return null;

        validator.validate(clazz);

        MethodDescriptor targetDescriptor = ReflectionMethodDescriptor.of(method);

        for (EventSubscriberInvoker<T, ?> invokerObj : factory.invokers()) {
            if (!invokerObj.descriptor().equals(targetDescriptor)) continue;

            @SuppressWarnings("unchecked") // bind E to base event type, safe as not returned; not needed tho.
            EventSubscriberInvoker<T, E> typedInvoker = (EventSubscriberInvoker<T, E>) invokerObj;
            return buildHandlerFactory(method, typedInvoker);
        }
        return null;
    }

    private <T, E extends Event> HandlerFactory<E> buildHandlerFactory(Method method, EventSubscriberInvoker<T, E> typedInvoker) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        return switch (typedInvoker) {
            case InstanceSubscriberInvoker<T, E> invoker -> {
                if (isStatic)
                    throw new IllegalStateException("Method " + method + " is static, but matched instance invoker " + invoker.getClass().getName());
                yield handlerFactoryProvider.create(invoker.priority(), invoker.ignoreCancelled(), obj -> {
                    @SuppressWarnings("unchecked")
                    T typedObj = (T) obj;
                    return invoker.create(typedObj);
                });
            }
            case StaticSubscriberInvoker<T, E> invoker -> {
                if (!isStatic)
                    throw new IllegalStateException("Method " + method + " is non-static, but matched static invoker " + invoker.getClass().getName());
                yield handlerFactoryProvider.create(invoker.priority(), invoker.ignoreCancelled(), obj -> invoker.create());
            }
        };
    }
}