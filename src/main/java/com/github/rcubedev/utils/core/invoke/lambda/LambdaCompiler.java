package com.github.rcubedev.utils.core.invoke.lambda;

import com.github.rcubedev.utils.event.impl.subscriber.MethodKey;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@UnitTestIgnored
public class LambdaCompiler<T> {

    // LambdaKey.functionalInterface() -> Class<T> & LambdaKey.methodType().clazz() -> Class<U> == Factory<T, U> --> generic is the same.
    private static final Map<LambdaKey, Factory<?, ?>> GLOBAL_CACHE = new ConcurrentHashMap<>();

    private final MethodHandles.Lookup lookup;
    private final Class<T> functionalInterface;
    private final MethodType samMethodType;
    private final String samMethodName;

    private LambdaCompiler(@NotNull MethodHandles.Lookup lookup, @NotNull Class<T> functionalInterface,
                           @NotNull SamInfo samInfo) {
        this.lookup = lookup;
        this.functionalInterface = functionalInterface;
        this.samMethodName = samInfo.name();
        this.samMethodType = samInfo.type();
    }

    public static <T> LambdaCompiler<T> create(@NotNull MethodHandles.Lookup lookup, @NotNull Class<T> functionalInterface) {
        return new LambdaCompiler<>(lookup, functionalInterface, findSam(functionalInterface));
    }

    public static <T> LambdaCompiler<T> create(@NotNull MethodHandles.Lookup lookup, @NotNull Class<T> functionalInterface,
                                               @NotNull SamInfo samInfo) {
        return new LambdaCompiler<>(lookup, functionalInterface, samInfo);
    }

    public <U> @NotNull Factory<T, U> compile(@NotNull Method method) {
        MethodHandle handle;
        try {
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), lookup);
            handle = privateLookup.unreflect(method);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create lambda factory for " + method, t);
        }
        @SuppressWarnings("unchecked")
        Class<U> declaringClass = (Class<U>) method.getDeclaringClass();
        return compile(new LambdaTarget<>(handle, method, declaringClass));
    }

    public <U> @NotNull Factory<T, U> compile(@NotNull LambdaTarget<U> target) {
        LambdaKey key = new LambdaKey(new MethodKey(target.method()), functionalInterface);

        Factory<?, ?> uncastFactory = GLOBAL_CACHE.computeIfAbsent(key, k -> {
            try {
                return doCompile(target);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to create lambda factory for " + target.method(), t);
            }
        });
        @SuppressWarnings("unchecked")
        Factory<T, U> factory = (Factory<T, U>) uncastFactory;
        return factory;
    }

    private <U> @NotNull Factory<T, U> doCompile(@NotNull LambdaTarget<U> target) {

        Method method = target.method();
        Class<U> clazz = target.declaringClass();
        if (clazz != method.getDeclaringClass()) throw new IllegalArgumentException("target has different declaring class.");
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        MethodType creationType = isStatic
                ? MethodType.methodType(functionalInterface)
                : MethodType.methodType(functionalInterface, clazz);
        MethodHandle impl = target.invoke();

        try {
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    samMethodName,
                    creationType,
                    samMethodType,
                    impl,
                    impl.type()
            );

            MethodHandle factoryHandle = site.getTarget();
            if (isStatic) {
                @SuppressWarnings("unchecked")
                T instance = (T) factoryHandle.invokeExact();
                return ignored -> instance;
            }

            factoryHandle = factoryHandle.asType(factoryHandle.type().changeParameterType(0, Object.class).changeReturnType(Object.class));
            MethodHandle finalFactoryHandle = factoryHandle;
            return instance -> (T) finalFactoryHandle.invokeExact(instance);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create lambda for " + target.method(), t);
        }
    }

    private static SamInfo findSam(Class<?> functionalInterface) {
        if (!functionalInterface.isInterface())
            throw new IllegalArgumentException("functionalInterface must be an interface");
        List<Method> absMethods = Arrays.stream(functionalInterface.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .filter(m -> !m.isDefault())
                .filter(m -> !isFromObject(m))
                .toList();
        if (absMethods.size() != 1)
            throw new IllegalArgumentException("Expected exactly one abstract method, found " + absMethods.size());
        Method method = absMethods.getFirst();
        return new SamInfo(method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()));
    }

    private static boolean isFromObject(Method m) {
        try {
            Object.class.getMethod(m.getName(), m.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @FunctionalInterface
    public interface Factory<T, U> {
        T create(@Nullable U instance) throws Throwable;
    }

    @UnitTestIgnored
    public record SamInfo(String name, MethodType type) {}

    @UnitTestIgnored
    public record LambdaTarget<T>(MethodHandle invoke, Method method, Class<T> declaringClass) {}

    @UnitTestIgnored
    public record LambdaKey(MethodKey methodKey, Class<?> functionalInterface) {}
}
