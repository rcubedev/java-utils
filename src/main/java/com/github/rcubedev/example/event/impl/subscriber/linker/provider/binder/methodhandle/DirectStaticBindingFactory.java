package com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.methodhandle;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public final class DirectStaticBindingFactory<T extends Event> implements BindingFactory<T> {

    private final EventProcessor<T> processor;

    public DirectStaticBindingFactory(Class<T> eventType, Method method, MethodHandle handle) {
        this.processor = event -> {
            try {
                handle.invokeExact(event);
            } catch (Error error) {
                throw error;
            } catch (Throwable t) {
                throw new RuntimeException("(MethodHandle) Static invocation failed for " + method, t);
            }
        };
    }

    @Override
    public EventProcessor<T> create(Object target) {
        return processor;
    }

    @FunctionalInterface
    public interface Provider {
        <T extends Event> DirectStaticBindingFactory<T> create(Class<T> eventType, Method method, MethodHandle handle);
    }
}