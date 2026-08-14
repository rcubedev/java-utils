package com.github.rcubedev.utils.event.impl.subscriber;

import com.github.rcubedev.utils.event.api.*;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.subscriber.validation.MethodValidator;
import com.github.rcubedev.utils.event.impl.subscriber.linker.LinkerEngine;
import com.github.rcubedev.utils.event.impl.subscriber.linker.MethodLinker;
import com.github.rcubedev.utils.event.impl.subscriber.linker.RuntimeLinkageEngine;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.HandlerInstantiationException;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventHandlerCompiler<B extends Event> {

    // MethodKey.type().parameterType(0) -> Class<T> == HandlerFactory<T> --> generic is the same.
    private static final Map<MethodKey, HandlerFactory<?>> CLASS_METAFACTORIES = new ConcurrentHashMap<>();

    private final LinkerEngine linkageEngine;

    @UnitTestIgnored
    public EventHandlerCompiler() {
        this(new RuntimeLinkageEngine());
    }

    EventHandlerCompiler(LinkerEngine linkageEngine) {
        this.linkageEngine = linkageEngine;
    }

    //fixme should validate if Identity can register for method if in cache
    public void registerListener(@Nullable Object instance, Method method, Identity identity, Registrar<B> registrar) {
        registerListener0(instance, method, identity, registrar);
    }

    private <E extends B> void registerListener0(@Nullable Object instance, Method method, Identity identity, Registrar<B> registrar) {

        MethodValidator<B> validationHook = registrar.methodValidator();
        if (!validationHook.isCompatible(method)) {
            validationHook.validate(method); // blow up
            return;
        }
        boolean isStatic = instance == null;

        @SuppressWarnings("unchecked")
        HandlerFactory<E> handlerFactory = (HandlerFactory<E>) CLASS_METAFACTORIES.computeIfAbsent(
                new MethodKey(method), k -> compile(method, identity, validationHook));

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

    private HandlerFactory<? extends B> compile(Method method, Identity identity, MethodValidator<B> validationHook) {
        Class<? extends B> paramType = validationHook.validate(method);
        MethodLinker<? extends B> linker = new MethodLinker<>(method, paramType, identity, linkageEngine);
        return linker.compile();
    }
}
