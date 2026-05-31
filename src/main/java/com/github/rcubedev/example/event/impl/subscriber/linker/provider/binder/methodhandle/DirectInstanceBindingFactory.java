package com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.methodhandle;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public final class DirectInstanceBindingFactory<T extends Event> implements BindingFactory<T> {

    private final Method method;
    private final MethodHandle handle;

    public DirectInstanceBindingFactory(Class<T> eventType, Method method, MethodHandle handle) {
        this.method = method;
        this.handle = handle;
    }

    @Override
    public EventProcessor<T> create(Object target) {
        return event -> {
            try {
                handle.invokeExact(target, event);
            } catch (Error error) {
                throw error;
            } catch (Throwable t) {
                throw new RuntimeException("(MethodHandle) Instance invocation failed for " + method, t);
            }
        };
    }

    @FunctionalInterface
    public interface Provider {
        <T extends Event> DirectInstanceBindingFactory<T> create(Class<T> eventType, Method method, MethodHandle handle);
    }
}