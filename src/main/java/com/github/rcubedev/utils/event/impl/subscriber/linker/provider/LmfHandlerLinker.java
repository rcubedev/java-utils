package com.github.rcubedev.utils.event.impl.subscriber.linker.provider;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.impl.processor.UnboundProcessor;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.StructuralLinkageException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.metafactory.HandleBindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.metafactory.StaticBindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.metafactory.WeakBindingFactory;
import com.github.rcubedev.utils.test.UnitTestIgnored;

import java.lang.invoke.*;

public class LmfHandlerLinker extends AbstractMethodHandleLinker {

    private final HandleBindingFactory.Provider handleFactoryProvider;
    private final StaticBindingFactory.Provider staticFactoryProvider;
    private final WeakBindingFactory.Provider weakFactoryProvider;

    @UnitTestIgnored
    public LmfHandlerLinker() {
        this(HandleBindingFactory::new, StaticBindingFactory::new, WeakBindingFactory::new);
    }

    LmfHandlerLinker(HandleBindingFactory.Provider handleFactoryProvider, StaticBindingFactory.Provider staticFactoryProvider, WeakBindingFactory.Provider weakFactoryProvider) {
        this.handleFactoryProvider = handleFactoryProvider;
        this.staticFactoryProvider = staticFactoryProvider;
        this.weakFactoryProvider = weakFactoryProvider;
    }

    @Override
    protected <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        Class<T> paramType = context.paramType();
        Class<?> targetClass = context.targetClass();

        boolean isStatic = unreflected.isStatic();
        MethodHandles.Lookup lookup = unreflected.lookup();
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
                return staticFactoryProvider.create(paramType, factoryHandle);
            }

            MethodHandle bridged = MethodHandles.explicitCastArguments(factoryHandle, factoryHandle.type().changeParameterType(0, Object.class));
            return handleFactoryProvider.create(paramType, bridged);
        } catch (StructuralLinkageException sle) {
            throw sle;
        } catch (Exception e) {
            throw new StructuralLinkageException("(LambdaMetafactory) Failed to create lambda", e);
        }
    }

    @Override
    protected <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        Class<T> paramType = context.paramType();
        Class<?> targetClass = context.targetClass();

        MethodHandles.Lookup lookup = unreflected.lookup();
        MethodHandle handle = unreflected.handle();

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup, "process",
                    MethodType.methodType(UnboundProcessor.class),
                    MethodType.methodType(void.class, Object.class, Event.class),
                    handle, MethodType.methodType(void.class, targetClass, paramType)
            );

            return weakFactoryProvider.create(paramType, site.getTarget());
        } catch (StructuralLinkageException sle) {
            throw sle;
        } catch (Exception e) {
            throw new StructuralLinkageException("(LambdaMetafactory) Failed to create weak lambda", e);
        }
    }
}
