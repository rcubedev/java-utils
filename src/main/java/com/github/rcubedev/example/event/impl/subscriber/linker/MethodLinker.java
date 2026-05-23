package com.github.rcubedev.example.event.impl.subscriber.linker;

import com.github.rcubedev.example.event.api.*;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.HandlerFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodLinker<T extends Event> {
    private final LinkageContext<T> context;
    private final Class<?> targetClass;
    private final boolean isStatic;
    private final LinkerEngine linker;

    public MethodLinker(Method method, Class<T> paramType, LinkerEngine linker) {
        this(MethodHandles.lookup(), method, paramType, linker);
    }

    MethodLinker(MethodHandles.Lookup lookup, Method method, Class<T> paramType, LinkerEngine linker) {
        this.linker = linker;
        this.targetClass = method.getDeclaringClass();
        this.isStatic = Modifier.isStatic(method.getModifiers());
        this.context = new LinkageContext<>(lookup, method, paramType);
    }

    public HandlerFactory<T> compile() {
        Method method = context.method();
        SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
        boolean isWeak = method.isAnnotationPresent(Weak.class) || targetClass.isAnnotationPresent(Weak.class);

        try {
            BindingFactory<T> factory = isWeak ? createWeak() : createStrong();
            return new HandlerFactory<>(annotation.priority(), annotation.ignoreCancelled(), factory);
        } catch (ModuleAccessException e) {
            throw new IllegalArgumentException("Cannot access @SubscribeEvent method due to module restrictions: " + method, e);
        } catch (MemberAccessException e) {
            throw new IllegalArgumentException("Cannot access @SubscribeEvent method: " + method, e);
        } catch (StructuralLinkageException e) {
            throw new RuntimeException("Failed to link handler for @SubscribeEvent method: " + method, e);
        }

        // BindingFactory<T> factory = isWeak ? createWeak() : createStrong();
        // return new HandlerFactory<>(annotation.priority(), annotation.ignoreCancelled(), factory);
    }

    private BindingFactory<T> createStrong() throws StructuralLinkageException, ModuleAccessException, MemberAccessException {
        return linker.linkStrong(context);
    }

    private BindingFactory<T> createWeak() throws StructuralLinkageException, ModuleAccessException, MemberAccessException {
        if (isStatic) {
            throw new IllegalArgumentException("""
                            Expected @SubscribeEvent method %s to NOT be static
                            because it was registered as a weak listener.
                            Either make the method non-static, or remove the @Weak annotation.
                            """.formatted(context.method()));
        }

        return linker.linkWeak(context);
    }

    /*private BindingFactory<E> createStrong() {
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
                EventProcessor<E> processor = (EventProcessor<E>) factoryHandle.invokeExact();
                return target -> processor;
            }

            MethodHandle bridged = factoryHandle.asType(factoryHandle.type().changeParameterType(0, Object.class));
            return target -> {
                @SuppressWarnings("unchecked")
                EventProcessor<E> processor = (EventProcessor<E>) bridged.invokeExact(target);
                return processor;
            };
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lambda for " + method, e);
        }
    }

    private BindingFactory<E> createWeak() {
        if (isStatic) {
            throw new IllegalArgumentException("""
                            Expected @SubscribeEvent method %s to NOT be static
                            because it was registered as a weak listener.
                            Either make the method non-static, or remove the @Weak annotation.
                            """.formatted(method));
        }

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup, "process",
                    MethodType.methodType(UnboundProcessor.class),
                    MethodType.methodType(void.class, Object.class, Event.class),
                    handle, MethodType.methodType(void.class, targetClass, paramType)
            );
            MethodHandle factoryHandle = site.getTarget();

            @SuppressWarnings("unchecked")
            UnboundProcessor<Object, E> unbound = (UnboundProcessor<Object, E>) factoryHandle.invokeExact();
            return instance -> new WeakEventProcessor<>(instance, unbound);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lambda for " + method, e);
        }
    }*/
}
