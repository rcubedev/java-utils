package com.github.rcubedev.utils.event.impl.subscriber.compiled;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.subscriber.validation.ClassValidator;
import com.github.rcubedev.utils.event.impl.subscriber.HandlerFactory;
import com.github.rcubedev.utils.event.impl.subscriber.MethodKey;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MethodHandlerCache {

    private final Map<MethodKey, Optional<HandlerFactory<?>>> cache = new ConcurrentHashMap<>();
    private final MethodHandlerResolver resolver;

    public MethodHandlerCache(MethodHandlerResolver resolver) {
        this.resolver = resolver;
    }

    public <E extends Event> @Nullable HandlerFactory<E> getOrCompute(Class<?> clazz, Method method, ClassValidator validator) {
        Optional<HandlerFactory<?>> handlerOpt = cache.computeIfAbsent(
                new MethodKey(method),
                k -> Optional.ofNullable(resolver.resolve(clazz, method, validator)));

        @SuppressWarnings("unchecked")
        HandlerFactory<E> handlerFactory = (HandlerFactory<E>) handlerOpt.orElse(null);
        return handlerFactory;
    }
}