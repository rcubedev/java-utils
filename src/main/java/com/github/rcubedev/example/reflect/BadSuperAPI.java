package com.github.rcubedev.example.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

// fixme incomplete. this is kind of a waste of time as why do you ever need to call a super super method; sorta
//  violates OO principles; instead rethink your design.
// todo possibly get the MethodHandles.Lookup.IMPL_LOOKUP via JNI (can't use FFM as not Java 22+) so this will never fail.
@Deprecated
public class BadSuperAPI {
    // Cache handles to avoid expensive Lookups
    // fixme cache the hierarchy so it doesn't need to lookup the cache key
    private static final Map<JlsReflectionHelper.CacheKey, MethodHandle> CACHE = new ConcurrentHashMap<>();

    /**
     * Calls a method from a specific ancestor, skipping all intermediate overrides.
     * If {@code ancestor} is an interface, this will look for a direct method in {@code ancestor}.
     * Otherwise, it will search for superclasses above ancestor for the method, if it cannot be found in {@code ancestor}.
     *
     * @param instance The object to call the method on (e.g., a SubSubClass)
     * @param ancestor the specific class whose code you want to run (e.g., SuperClass).
     * @param method a {@link Method} object representing the signature (from any class in the chain) fixme has to be ancestor or sub
     * @param lookup todo
     * @param args The arguments for the call
     */
    public static Object callAncestor(Object instance, Class<?> ancestor, Method method, MethodHandles.Lookup lookup, IArgument<?>... args) throws Throwable {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Method " + method.getName() + " is a static method, and therefore has no super methods.");
        }

        if (!(method.getDeclaringClass().isAssignableFrom(instance.getClass()))) {
            throw new IllegalArgumentException("Method " + method.getName() + " is not declared in " + instance.getClass().getName());
        }

        Class<?> clazz = instance.getClass();
        if (!ancestor.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Incompatible types: " + clazz.getName() + " is not a " + ancestor.getName());
        }

        if (method.isSynthetic()) {
            throw new IllegalArgumentException("Cannot super-call a synthetic method: " + method);
        }

        // Find the appropriate return type and cache key values
        Method m = findMethod(ancestor, method);
        JlsReflectionHelper.CacheKey cacheKey = JlsReflectionHelper.CacheKey.create(m.getDeclaringClass(), m.getName(), m.getReturnType(), List.of(m.getParameterTypes()), lookup);
        MethodHandle mh = CACHE.computeIfAbsent(cacheKey, k -> {
            try {
                MethodHandles.Lookup privilegedLookup;
                try {
                    privilegedLookup = MethodHandles.privateLookupIn(k.target(), lookup);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Provided lookup cannot access " + k.target().getName(), e);
                } catch (SecurityException e) {
                    throw new IllegalStateException("SecurityManager denied access to " + k.target().getName(), e);
                }

                // Class<?>[] paramArray = k.params().toArray(Class[]::new);
                // MethodType mt = MethodType.methodType(k.returnType(), paramArray);

                // Create the invokespecial handle
                // This can still fail if the method is a constructor (verif i think this is false)
                // workaround: define specialCaller as the target as invoking findSpecial with real target will only let you call the super of target
                // return privilegedLookup.findSpecial(k.target(), k.methodName(), mt, k.target());
                return privilegedLookup.unreflectSpecial(m, k.target());

            } catch (/*NoSuchMethodException | */IllegalAccessException e) {
                throw new IllegalStateException("Failed to bind special handle for " + k, e);
            }
        });

        System.out.println("Attempting to invoke handle: " + mh + ". Method was: " + m);
        return JlsReflectionHelper.invoke(mh, args);
    }

    // only checks for superclass methods
    private static @NotNull Method findMethod(Class<?> target, Method method) {
        return findMethod(target, method, method.getParameterTypes());
    }

    private static @NotNull Method findMethod(@NotNull Class<?> target, @NotNull Method method, @NotNull Class<?>[] paramArray) {
        Class<?> currentTarget = target;
        while (currentTarget != null) {
            int count = 0;
            Method selected = null;

            for (Method m : currentTarget.getDeclaredMethods()) {
                // incase return type is overloaded. excl bridge methods as they are always less specific
                if (m.isBridge() || Modifier.isStatic(m.getModifiers()) || Modifier.isAbstract(m.getModifiers()) ||
                        !m.getName().equals(method.getName()) || !m.getReturnType().isAssignableFrom(method.getReturnType())
                        || !Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) continue;
                count++;
                if (count > 1) {
                    throw new IllegalStateException("Method " + method.getName() + " is ambiguous for " + Arrays.toString(paramArray));
                }
                selected = m;
            }

            if (selected != null) return selected;
            currentTarget = currentTarget.getSuperclass();
        }
        throw new IllegalArgumentException("Method " + method + " not found in " + target.getName());
    }
}