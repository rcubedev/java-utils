package com.github.rcubedev.example.event.impl.subscriber.linker.provider;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.processor.UnboundProcessor;
import com.github.rcubedev.example.event.impl.processor.WeakEventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.HandlerInstantiationException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;

import java.lang.invoke.*;

public class LmfHandlerLinker extends AbstractMethodHandleLinker {

    @Override
    protected <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        Class<T> paramType = context.paramType();
        Class<?> targetClass = context.targetClass();

        boolean isStatic = unreflected.isStatic();
        MethodHandles.Lookup lookup = unreflected.privateLookup();
        MethodHandle handle = unreflected.handle();

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup, "process",
                    isStatic ? MethodType.methodType(EventProcessor.class) : MethodType.methodType(EventProcessor.class, targetClass),
                    MethodType.methodType(void.class, Event.class),
                    handle, MethodType.methodType(void.class, paramType)
            );
            MethodHandle factoryHandle = site.getTarget();

            if (isStatic) {
                @SuppressWarnings("unchecked")
                EventProcessor<T> processor = (EventProcessor<T>) factoryHandle.invokeExact();
                return target -> processor;
            }

            MethodHandle bridged = factoryHandle.asType(factoryHandle.type().changeParameterType(0, Object.class));
            return target -> {
                try {
                    @SuppressWarnings("unchecked")
                    EventProcessor<T> processor = (EventProcessor<T>) bridged.invokeExact(target);
                    return processor;
                } catch (Error error) {
                    throw error;
                } catch (Throwable t) {
                    throw new HandlerInstantiationException("(LambdaMetaFactory) Failed to instantiate lambda site for target", t);
                }
            };
        } catch (Error error) {
            throw error;
        } catch (Throwable t) {
            throw new StructuralLinkageException("(LambdaMetafactory) Failed to create lambda", t);
        }
    }

    @Override
    protected <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        Class<T> paramType = context.paramType();
        Class<?> targetClass = context.targetClass();

        MethodHandles.Lookup lookup = unreflected.privateLookup();
        MethodHandle handle = unreflected.handle();

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup, "process",
                    MethodType.methodType(UnboundProcessor.class),
                    MethodType.methodType(void.class, Object.class, Event.class),
                    handle, MethodType.methodType(void.class, targetClass, paramType)
            );

            @SuppressWarnings("unchecked")
            UnboundProcessor<Object, T> unbound = (UnboundProcessor<Object, T>) site.getTarget().invokeExact();
            return target -> new WeakEventProcessor<>(target, unbound);
        } catch (Error error) {
            throw error;
        } catch (Throwable t) {
            throw new StructuralLinkageException("(LambdaMetafactory) Failed to create weak lambda", t);
        }
    }
}
