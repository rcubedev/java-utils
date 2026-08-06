package com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.metafactory;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.StructuralLinkageException;

import java.lang.invoke.MethodHandle;

public final class StaticBindingFactory<T extends Event> implements BindingFactory<T> {

    private final EventProcessor<T> processor;

    public StaticBindingFactory(Class<T> eventType, MethodHandle factory) throws StructuralLinkageException {
        if (factory.type().parameterCount() != 0) {
            throw new IllegalArgumentException(
                    "Static factory MethodHandle must have zero arguments. Found: " + factory.type());
        }
        if (factory.type().returnType() != EventProcessor.class) {
            throw new IllegalArgumentException(
                    "Static factory MethodHandle must return an EventProcessor. Found: " + factory.type().returnType().getName()
            );
        }

        this.processor = create(factory);
    }

    @Override
    public EventProcessor<T> create(Object target) {
        return processor;
    }

    private static <T extends Event> EventProcessor<T> create(MethodHandle factory) throws StructuralLinkageException {
        try {
            @SuppressWarnings("unchecked")
            EventProcessor<T> processor = (EventProcessor<T>) factory.invokeExact();
            return processor;
        } catch (Error error) {
            throw error;
        } catch (Throwable t) {
            throw new StructuralLinkageException("(LambdaMetaFactory) Failed to create lambda", t);
        }
    }

    @FunctionalInterface
    public interface Provider {
        <T extends Event> StaticBindingFactory<T> create(Class<T> eventType, MethodHandle factory) throws StructuralLinkageException;
    }
}
