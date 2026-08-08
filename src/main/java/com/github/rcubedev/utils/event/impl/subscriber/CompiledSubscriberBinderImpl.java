package com.github.rcubedev.utils.event.impl.subscriber;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.api.subscriber.CompiledSubscriberBinder;
import com.github.rcubedev.utils.event.impl.subscriber.compiled.CompiledSubscriberCompiler;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

//todo add method & static binding
public final class CompiledSubscriberBinderImpl implements CompiledSubscriberBinder {

    private final CompiledSubscriberCompiler compiler;

    @UnitTestIgnored
    public CompiledSubscriberBinderImpl() {
        this(new CompiledSubscriberCompiler());
    }

    CompiledSubscriberBinderImpl(CompiledSubscriberCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public <B extends Event> List<Subscription> register(@NotNull Object target, @NotNull Identity identity,
                                                         @NotNull Registrar<B> registrar) {
        return switch (target) {
            case Method method -> this.compiler.bindMethod(method, identity, registrar);
            case Class<?> clazz -> this.compiler.bindStatic(clazz, identity, registrar);
            default -> this.compiler.bindInstance(target, identity, registrar);
        };
    }

    public static class Holder {
        public static final CompiledSubscriberBinderImpl INSTANCE = new CompiledSubscriberBinderImpl();
        private Holder() {}
    }
}
