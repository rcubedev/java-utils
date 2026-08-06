package com.github.rcubedev.utils.event.impl.subscriber.linker;

import com.github.rcubedev.utils.event.api.*;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.HandlerFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.StructuralLinkageException;
import com.github.rcubedev.utils.test.UnitTestIgnored;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// todo leaks state: creates real HandlerFactory
public class MethodLinker<T extends Event> {
    private final LinkageContext<T> context;
    private final Class<?> targetClass;
    private final boolean isStatic;
    private final LinkerEngine linker;
    private final HandlerFactory.Provider<T> handlerFactoryProvider;

    @UnitTestIgnored
    public MethodLinker(Method method, Class<T> paramType, Identity identity, LinkerEngine linker) {
        this(method.getDeclaringClass(), Modifier.isStatic(method.getModifiers()), linker,
                new LinkageContext<>(identity.lookup(), method, paramType), HandlerFactory::new);
    }

    MethodLinker(Class<?> targetClass, boolean isStatic, LinkerEngine linker, LinkageContext<T> context,
                 HandlerFactory.Provider<T> handlerFactoryProvider) {
        this.linker = linker;
        this.targetClass = targetClass;
        this.isStatic = isStatic;
        this.context = context;
        this.handlerFactoryProvider = handlerFactoryProvider;
    }

    public HandlerFactory<T> compile() {
        Method method = context.method();
        SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
        boolean isWeak = method.isAnnotationPresent(Weak.class) || targetClass.isAnnotationPresent(Weak.class);

        try {
            BindingFactory<T> factory = isWeak ? createWeak() : createStrong();
            return handlerFactoryProvider.create(annotation.priority(), annotation.ignoreCancelled(), factory);
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
}
