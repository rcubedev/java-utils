package com.example.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A high-fidelity reflection utility that emulates JLS §15.9 and §15.12.
 * Designed to mirror javac's compile-time overload resolution at runtime.
 */
//fixme future: swap to method handles?
public class JlsReflectionHelper {

    private static final Map<ConstructorKey, Constructor<?>> RESOLUTION_CACHE = new ConcurrentHashMap<>(); // fixme swap to weak keys and cache with expiry

    private static final BiMap<Class<?>, Class<?>> PRIMITIVE_LOOKUP = ImmutableBiMap.of(
            int.class, Integer.class,
            long.class, Long.class,
            boolean.class, Boolean.class,
            double.class, Double.class,
            float.class, Float.class,
            char.class, Character.class,
            byte.class, Byte.class,
            short.class, Short.class,
            void.class, Void.class // Added void for JLS completeness
    );

    private static final Map<Class<?>, Set<Class<?>>> WIDENING_PRIMITIVE = Map.of(
            byte.class,  Set.of(short.class, int.class, long.class, float.class, double.class),
            short.class, Set.of(int.class, long.class, float.class, double.class),
            char.class,  Set.of(int.class, long.class, float.class, double.class),
            int.class,   Set.of(long.class, float.class, double.class),
            long.class,  Set.of(float.class, double.class),
            float.class, Set.of(double.class)
    );

    /**
     * Instantiates a class by mimicking javac overload resolution.
     * @param clazz          The class to instantiate.
     * @param caller         The calling class context (for visibility).
     * @param args           The arguments containing values and compile time types.
     */
    @SuppressWarnings("unchecked")
    public static <T> T instantiate(@NotNull Class<T> clazz, @Nullable Class<?> caller,
                                    IArgument<?> @NotNull ... args) {

        if (clazz.isEnum()) throw new UnsupportedOperationException("JLS §8.9.2: Enum constructors are unreachable.");
        // System.out.println("ARGS: " + Arrays.toString(args));
        Objects.requireNonNull(clazz, "Class must not be null.");
        Objects.requireNonNull(args, "Arguments or the array must not be null.");

        // Use TypedClass from Arguments to preserve generic information
        TypedClass<?>[] argTypes = Arrays.stream(args)
                .map(arg -> Objects.requireNonNull(arg, "Argument must not be null."))
                .map(IArgument::getStaticType)
                .toArray(TypedClass<?>[]::new);

        // Check synthetic prepending for inner classes; not a full check as inner class ctor may have Outer (synthetic), Outer
        checkSignature(clazz, argTypes);

        ConstructorKey key = new ConstructorKey(clazz, argTypes, caller); // fixme why cache caller; doing so makes it recompute for each call, maybe instead also cache caller sep
        Constructor<T> ctor = (Constructor<T>) RESOLUTION_CACHE.computeIfAbsent(key, k -> {
            Constructor<T> found = findConstructor(clazz, argTypes, caller);
            if (found != null) found.setAccessible(true);
            return found;
        });

        if (ctor == null) {
            throw new NoSuchElementException("No JLS-compliant constructor found for " + clazz.getName() + ". Ctors: " + Arrays.toString(Arrays.stream(clazz.getDeclaredConstructors()).map(Constructor::toGenericString).toArray())); // fixme make an ex
        }

        return (T) invoke(ctor, args);
    }

    private static <T> Constructor<T> findConstructor(Class<T> clazz, TypedClass<?>[] argTypes, Class<?> caller) {
        List<Constructor<T>> ctors = Arrays.stream((Constructor<T>[]) clazz.getDeclaredConstructors())
                .filter(c -> isVisible(c, caller))
                .toList();

        // JLS §15.12.2: Phased Applicability Search
        List<Constructor<T>> matches = filter(ctors, argTypes, false, false); // Phase 1
        if (matches.isEmpty()) matches = filter(ctors, argTypes, true, false); // Phase 2
        if (matches.isEmpty()) matches = filter(ctors, argTypes, true, true);  // Phase 3
        return matches.isEmpty() ? null : resolveMostSpecific(matches);
    }

    private static <T> Constructor<T> resolveMostSpecific(List<Constructor<T>> matches) {
        if (matches.size() == 1) return matches.getFirst();

        Constructor<T> winner = matches.getFirst();
        for (int i = 1; i < matches.size(); i++) {
            Constructor<T> candidate = matches.get(i);
            if (isMoreSpecific(candidate, winner)) winner = candidate;
        }

        // Ambiguity Guard: Ensure winner is strictly more specific than all other matches
        for (Constructor<T> other : matches) {
            if (other != winner && !isMoreSpecific(winner, other)) {
                throw new RuntimeException("Ambiguous call: " + winner + " and " + other + " are both applicable.");
            }
        }
        return winner;
    }

    private static boolean isMoreSpecific(Constructor<?> c1, Constructor<?> c2) {
        Class<?>[] p1 = c1.getParameterTypes();
        Class<?>[] p2 = c2.getParameterTypes();

        if (c1.isVarArgs() != c2.isVarArgs()) return !c1.isVarArgs();

        int len = Math.min(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            if (!isPhase1Compatible(p2[i], p1[i])) return false;
        }

        if (c1.isVarArgs() && c2.isVarArgs()) {
            return isPhase1Compatible(p2[p2.length-1].getComponentType(), p1[p1.length-1].getComponentType());
        }
        return true;
    }

    private static boolean isVisible(@NotNull Constructor<?> ctor, @Nullable Class<?> caller) {
        int mod = ctor.getModifiers();
        if (Modifier.isPublic(mod)) return true;
        if (caller == null) return false;

        Class<?> declaring = ctor.getDeclaringClass();
        if (getTopLevelClass(declaring) == getTopLevelClass(caller)) return true; // private, protected & default

        boolean samePkg = declaring.getPackageName().equals(caller.getPackageName());
        if (Modifier.isProtected(mod)) return samePkg || declaring.isAssignableFrom(caller);
        return samePkg; // Package-private
    }

    private static boolean isCompatible(@NotNull Type target, @NotNull TypedClass<?> source, boolean allowBox) {

        Class<?> sourceRaw = source.getTypedClass();

        // Handle Primitives/Boxing first as TypedClass usually wraps them
        if (target instanceof Class<?> targetClass && targetClass.isPrimitive()) {
            if (sourceRaw == null) return false;
            if (sourceRaw == targetClass) return true;
            return WIDENING_PRIMITIVE.getOrDefault(sourceRaw, Collections.emptySet()).contains(targetClass);
        }

        if (allowBox) {
            // Allow TypedClass to handle the heavy lifting for objects and generic bounds
            return source.isAssignableTo(target);
        }

        // Phase 1: Only widening and identity (strict)
        return isPhase1Compatible(getRawClass(target), sourceRaw);
    }

    private static boolean isPhase1Compatible(Class<?> target, Class<?> source) {
        if (target.isAssignableFrom(source)) return true;
        return source.isPrimitive() && target.isPrimitive() &&
                WIDENING_PRIMITIVE.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    private static Object invoke(Constructor<?> ctor, IArgument<?>[] args) {
        try {
            // fixme check generic types
            Object[] rawArgs = Arrays.stream(args).map(IArgument::get).toArray();
            if (!ctor.isVarArgs()) return ctor.newInstance(rawArgs);

            int lastIdx = ctor.getParameterCount() - 1;
            Class<?> varargType = ctor.getParameterTypes()[lastIdx];


            // JLS §15.12.4.2: Varargs Array Identity
            Class<?> componentType = varargType.getComponentType();

            // If pre-packed via Argument
            if (args.length == ctor.getParameterCount() && args[lastIdx].getKind() == Argument.Kind.VAR_ARGS) {
                Object lastArg = rawArgs[lastIdx];
                if (lastArg == null || varargType.isAssignableFrom(lastArg.getClass())) {
                    return ctor.newInstance(rawArgs);
                }
            }

            // Else pack individual arguments
            int varArgLen = args.length - lastIdx;
            Object varArgArray = Array.newInstance(componentType, varArgLen);
            for (int i = 0; i < varArgLen; i++) Array.set(varArgArray, i, rawArgs[lastIdx + i]);

            Object[] combined = new Object[lastIdx + 1];
            System.arraycopy(rawArgs, 0, combined, 0, lastIdx);
            combined[lastIdx] = varArgArray;
            return ctor.newInstance(combined);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // --- Static Utility Helpers ---

    private static Class<?>[] inferStaticTypes(Object[] args) {
        return Arrays.stream(args).map(a -> a == null ? null : a.getClass()).toArray(Class[]::new);
    }

    /**
     * Ensures the argument types align with the physical signature of the constructor.
     * For non-static member classes, the JVM requires the first parameter to be
     * the enclosing instance (JLS §15.9.3).
     */
    private static TypedClass<?>[] checkSignature(Class<?> clazz, TypedClass<?>[] argTypes) {
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            Class<?> outer = clazz.getEnclosingClass();

            // not a full check; the inner class may have a ctor with the compiler generated outer class instance and
            // another outer class instance
            if (argTypes.length == 0 || !outer.isAssignableFrom(argTypes[0].getTypedClass())) { // instead of checking assignable from, maybe check equals?
                throw new IllegalArgumentException(String.format(
                        "Missing or invalid outer instance for inner class [%s]. The first argument must be an instance of [%s].",
                        clazz.getName(), outer.getName()
                ));
            }
        }
        return argTypes;
    }

    private static <T> List<Constructor<T>> filter(List<Constructor<T>> ctors, TypedClass<?>[] argTypes, boolean box, boolean var) {
        return ctors.stream().filter(c -> {
            Class<?>[] params = c.getParameterTypes();
            Type[] paramTypes = c.getGenericParameterTypes();
            if (!var && (c.isVarArgs() || c.getParameterCount() != argTypes.length)) return false;
            if (var && !c.isVarArgs()) return false;

            int fixedLimit = c.isVarArgs() ? params.length - 1 : params.length;
            if (var && argTypes.length < fixedLimit) return false;

            // Check fixed parameters
            for (int i = 0; i < fixedLimit; i++) {
                if (!isCompatible(paramTypes[i], argTypes[i], box)) return false;
            }

            // Check variable parameters ("over and over")
            if (var) {
                Type varargArrayType = paramTypes[fixedLimit];
                // Type componentType = getVarargComponentType(varargArrayType);
                for (int i = fixedLimit; i < argTypes.length; i++) {
                    if (!isCompatible(varargArrayType, argTypes[i], box)) return false;
                }
            }
            return true;
        }).toList();
    }

    private static Type getVarargComponentType(Type arrayType) {
        if (arrayType instanceof Class<?> cls) return cls.getComponentType();
        if (arrayType instanceof GenericArrayType gat) return gat.getGenericComponentType();
        throw new IllegalStateException("Expected array type for varargs: " + arrayType);
    }

    private static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?> cls) return cls;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        return Object.class; // Fallback for complex captures/variables in Phase 1
    }

    private static Class<?> getTopLevelClass(Class<?> clazz) {
        Class<?> top = clazz;
        while (top.getEnclosingClass() != null) top = top.getEnclosingClass();
        return top;
    }

    private static Class<?> getPrimitive(Class<?> wrapper) {
        return PRIMITIVE_LOOKUP.inverse().get(wrapper);
    }

    private record ConstructorKey(Class<?> clazz, TypedClass<?>[] argTypes, Class<?> caller) {}
}