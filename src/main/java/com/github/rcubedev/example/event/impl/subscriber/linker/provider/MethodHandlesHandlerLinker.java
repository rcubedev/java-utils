package com.github.rcubedev.example.event.impl.subscriber.linker.provider;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.processor.WeakEventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;

import java.lang.invoke.MethodHandle;

public class MethodHandlesHandlerLinker extends AbstractMethodHandleLinker {

    @Override
    protected <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        boolean isStatic = unreflected.isStatic();
        MethodHandle h = unreflected.handle();

        MethodHandle handle = isStatic
                    ? h.asType(h.type().changeParameterType(0, Event.class))
                    : h.asType(h.type().changeParameterType(0, Object.class).changeParameterType(1, Event.class));

        if (isStatic) {
            EventProcessor<T> processor = event -> {
                try {
                    handle.invokeExact(event);
                } catch (Error error) {
                    throw error;
                } catch (Throwable t) {
                    throw new RuntimeException("(MethodHandle) Static invocation failed for " + context.method(), t);
                }
            };
            return target -> processor;
        }

        return target -> {
            MethodHandle bound = handle.bindTo(target);
            return event -> {
                try {
                    bound.invokeExact(event);
                } catch (Error error) {
                    throw error;
                } catch (Throwable t) {
                    throw new RuntimeException("(MethodHandle) Instance invocation failed for " + context.method(), t);
                }
            };
        };
    }

    @Override
    protected <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        MethodHandle h = unreflected.handle();
        MethodHandle handle = h.asType(h.type().changeParameterType(0, Object.class).changeParameterType(1, Event.class));

        return target -> new WeakEventProcessor<>(target, (instance, event) -> {
            try {
                handle.invokeExact(instance, event);
            } catch (Error error) {
                throw error;
            } catch (Throwable t) {
                throw new RuntimeException("(MethodHandle) Weak invocation failed for " + context.method(), t);
            }
        });
    }
}
