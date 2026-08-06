package com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.methodhandle;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.impl.processor.WeakEventProcessor;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public final class DirectWeakBindingFactory<T extends Event> implements BindingFactory<T> {

    private final Method method;
    private final MethodHandle handle;
    private final WeakEventProcessor.Factory<Object, T> processorFactory;

    @UnitTestIgnored
    public DirectWeakBindingFactory(Class<T> eventType, Method method, MethodHandle handle) {
        this(method, handle, WeakEventProcessor::new);
    }

    DirectWeakBindingFactory(Method method, MethodHandle handle, WeakEventProcessor.Factory<Object, T> processorFactory) {
        this.method = method;
        this.handle = handle;
        this.processorFactory = processorFactory;
    }

    @Override
    public EventProcessor<T> create(Object target) {
        return this.processorFactory.create(target, (instance, event) -> {
            try {
                handle.invokeExact(instance, event);
            } catch (Error error) {
                throw error;
            } catch (Throwable t) {
                throw new RuntimeException("(MethodHandle) Weak invocation failed for " + method, t);
            }
        });
    }

    @FunctionalInterface
    public interface Provider {
        <T extends Event> DirectWeakBindingFactory<T> create(Class<T> eventType, Method method, MethodHandle handle);
    }
}