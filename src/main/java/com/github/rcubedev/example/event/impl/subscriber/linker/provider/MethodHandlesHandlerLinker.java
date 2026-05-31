package com.github.rcubedev.example.event.impl.subscriber.linker.provider;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.processor.WeakEventProcessor;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;
import com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.methodhandle.DirectInstanceBindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.methodhandle.DirectStaticBindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.methodhandle.DirectWeakBindingFactory;
import com.github.rcubedev.example.test.UnitTestIgnored;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class MethodHandlesHandlerLinker extends AbstractMethodHandleLinker {

    private final DirectInstanceBindingFactory.Provider instanceProvider;
    private final DirectStaticBindingFactory.Provider staticProvider;
    private final DirectWeakBindingFactory.Provider weakProvider;

    @UnitTestIgnored
    public MethodHandlesHandlerLinker() {
        this(DirectInstanceBindingFactory::new, DirectStaticBindingFactory::new, DirectWeakBindingFactory::new);
    }

    MethodHandlesHandlerLinker(DirectInstanceBindingFactory.Provider instanceProvider,
                               DirectStaticBindingFactory.Provider staticProvider,
                               DirectWeakBindingFactory.Provider weakProvider) {
        this.instanceProvider = instanceProvider;
        this.staticProvider = staticProvider;
        this.weakProvider = weakProvider;
    }

    @Override
    protected <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        Class<T> paramType = context.paramType();
        boolean isStatic = unreflected.isStatic();
        MethodHandle h = unreflected.handle();

        if (isStatic) {
            MethodHandle handle = MethodHandles.explicitCastArguments(h, h.type().changeParameterType(0, Event.class));
            return staticProvider.create(paramType, context.method(), handle);
        }

        MethodHandle handle = MethodHandles.explicitCastArguments(h, h.type()
                .changeParameterType(0, Object.class).changeParameterType(1, Event.class));
        return instanceProvider.create(paramType, context.method(), handle);
    }

    @Override
    protected <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
        MethodHandle h = unreflected.handle();
        MethodHandle handle = MethodHandles.explicitCastArguments(h, h.type().changeParameterType(0, Object.class).changeParameterType(1, Event.class));

        return weakProvider.create(context.paramType(), context.method(), handle);
    }
}
