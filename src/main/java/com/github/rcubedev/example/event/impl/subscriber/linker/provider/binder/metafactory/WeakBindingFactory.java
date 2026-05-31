package com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.metafactory;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.processor.UnboundProcessor;
import com.github.rcubedev.example.event.impl.processor.WeakEventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;
import com.github.rcubedev.example.test.UnitTestIgnored;

import java.lang.invoke.MethodHandle;

public final class WeakBindingFactory<T extends Event> implements BindingFactory<T> {

    private final UnboundProcessor<Object, T> unbound;
    private final WeakEventProcessor.Factory<Object, T> processorFactory;

    @UnitTestIgnored
    public WeakBindingFactory(Class<T> eventType, MethodHandle factory) throws StructuralLinkageException {
        this(factory, WeakEventProcessor::new);
    }

    WeakBindingFactory(MethodHandle factory, WeakEventProcessor.Factory<Object, T> processorFactory) throws StructuralLinkageException {
        this.unbound = create(verify(factory));
        this.processorFactory = processorFactory;
    }

    @Override
    public EventProcessor<T> create(Object target) {
        return this.processorFactory.create(target, unbound);
    }

    private static <T extends Event> UnboundProcessor<Object, T> create(MethodHandle factory) throws StructuralLinkageException {
        try {
            @SuppressWarnings("unchecked")
            UnboundProcessor<Object, T> processor = (UnboundProcessor<Object, T>) factory.invokeExact();
            return processor;
        } catch (Error error) {
            throw error;
        } catch (Throwable t) {
            throw new StructuralLinkageException("(LambdaMetaFactory) Failed to create weak lambda", t);
        }
    }

    private static MethodHandle verify(MethodHandle factory) {
        if (factory.type().parameterCount() != 0) {
            throw new IllegalArgumentException(
                    "Weak factory MethodHandle must have zero arguments. Found: " + factory.type());
        }
        if (factory.type().returnType() != UnboundProcessor.class) {
            throw new IllegalArgumentException(
                    "Weak factory MethodHandle must return an UnboundProcessor. Found: " + factory.type().returnType().getName()
            );
        }
        return factory;
    }

    @FunctionalInterface
    public interface Provider {
        <T extends Event> WeakBindingFactory<T> create(Class<T> eventType, MethodHandle factory) throws StructuralLinkageException;
    }
}
