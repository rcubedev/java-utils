package com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.metafactory;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.HandlerInstantiationException;

import java.lang.invoke.MethodHandle;

public final class HandleBindingFactory<T extends Event> implements BindingFactory<T> {

    private final MethodHandle bridged;

    public HandleBindingFactory(Class<T> eventType, MethodHandle bridged) {
        if (bridged.type().parameterCount() != 1 || bridged.type().parameterType(0) != Object.class)
            throw new IllegalArgumentException(
                    "Bridged factory MethodHandle must have a single argument of type Object. Found: " + bridged.type());
        if (!(EventProcessor.class == bridged.type().returnType())) {
            throw new IllegalArgumentException(
                    "Bridged factory MethodHandle must return an EventProcessor. Found: " + bridged.type().returnType().getName()
            );
        }
        this.bridged = bridged;
    }

    @Override
    public EventProcessor<T> create(Object target) throws HandlerInstantiationException {
        try {
            @SuppressWarnings("unchecked")
            EventProcessor<T> processor = (EventProcessor<T>) bridged.invokeExact(target);
            return processor;
        } catch (Error error) {
            throw error;
        } catch (Throwable t) {
            throw new HandlerInstantiationException("(LambdaMetaFactory) Failed to instantiate lambda site for target", t);
        }
    }

    @FunctionalInterface
    public interface Provider {
        <T extends Event> HandleBindingFactory<T> create(Class<T> eventType, MethodHandle bridged);
    }
}
