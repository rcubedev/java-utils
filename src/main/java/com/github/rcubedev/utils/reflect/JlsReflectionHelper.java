package com.github.rcubedev.utils.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A high-fidelity reflection utility that emulates JLS §15.9 and §15.12.
 * Designed to mirror javac's compile-time overload resolution at runtime.
 */
// todo needs optimizing
public class JlsReflectionHelper<T> {

    private static final Map<CacheKey, Optional<MethodHandle>> RESOLUTION_CACHE = new ConcurrentHashMap<>(); // fixme swap to weak keys and cache with expiry?

    private static final Map<Class<?>, Set<Class<?>>> WIDENING_PRIMITIVE = Map.of(
            byte.class,  Set.of(short.class, int.class, long.class, float.class, double.class),
            short.class, Set.of(int.class, long.class, float.class, double.class),
            char.class,  Set.of(int.class, long.class, float.class, double.class),
            int.class,   Set.of(long.class, float.class, double.class),
            long.class,  Set.of(float.class, double.class),
            float.class, Set.of(double.class)
    );

    private final Class<T> target;
    private final MethodHandles.Lookup lookup;

    private JlsReflectionHelper(Class<T> target, MethodHandles.Lookup lookup) {
        try {
            this.target = lookup.accessClass(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Lookup " + lookup + " cannot access " + target); // todo make a custom ex
        }
        this.lookup = lookup;
    }

    // default to public access
    public static <T> JlsReflectionHelper<T> getInstance(Class<T> target) {
        return getInstance(target, MethodHandles.publicLookup());
    }

    /**
     * Todo javadoc
     * If targeting an accessible ctor/etc prefer the overload or pass in MethodHandles.publicLook()
     * @param lookup
     * @return
     */
    public static <T> JlsReflectionHelper<T> getInstance(Class<T> clazz, MethodHandles.Lookup lookup) {
        return new JlsReflectionHelper<>(clazz, lookup);
    }

    /**
     * Instantiates a class by mimicking javac overload resolution.
     * @param args The arguments containing values and compile time types.
     */
    @SuppressWarnings("unchecked")
    public T instantiate(IArgument<?> @NotNull ... args) {

        if (target.isEnum()) throw new UnsupportedOperationException("JLS §8.9.2: Enum constructors are unreachable.");
        Objects.requireNonNull(target, "Class must not be null.");
        Objects.requireNonNull(args, "Arguments or the array must not be null.");

        TypedClass<?>[] argTypes = new TypedClass[args.length];
        for (int i = 0; i < args.length; i++) {
            IArgument<?> arg = args[i];
            Objects.requireNonNull(arg, "Argument must not be null.");
            argTypes[i] = arg.getStaticType();
        }

        // Check synthetic prepending for inner classes; not a full check as inner class ctor may have Outer (synthetic), Outer
        checkSignature(target, argTypes);

        Class<?>[] paramArray = new Class<?>[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            paramArray[i] = argTypes[i].getTypedClass();
        }
        List<Class<?>> params = List.of(paramArray);
        CacheKey key = CacheKey.create(target, "new", target, params, lookup);
        Optional<MethodHandle> handle = RESOLUTION_CACHE.computeIfAbsent(key, k -> Optional.ofNullable(findMethodHandle(target, args, lookup)));

        if (handle.isEmpty()) {
            // throw new NoSuchElementException("No JLS-compliant constructor found for " + target.getName()); // fixme make an ex
            throw new NoSuchElementException("No JLS-compliant constructor found for " + target.getName() + ". Arguments: " + Arrays.stream(args).map(a -> a.getStaticType().toString()).collect(Collectors.joining(", "))); // fixme make an ex
        }

        return (T) invoke(handle.get(), args);
    }

    private static <T> MethodHandle findMethodHandle(Class<T> clazz, IArgument<?>[] args, MethodHandles.Lookup lookup) {
        @SuppressWarnings("unchecked")
        List<Constructor<T>> ctors = Arrays.asList((Constructor<T>[]) clazz.getDeclaredConstructors());

        // JLS §15.12.2: Phased Applicability Search
        List<Constructor<T>> matches = filter(ctors, args, false, false); // Phase 1
        if (matches.isEmpty()) matches = filter(ctors, args, true, false); // Phase 2
        if (matches.isEmpty()) matches = filter(ctors, args, true, true);  // Phase 3

        List<MethodHandleCtorPair<T>> matchingHandles = matches.isEmpty() ? List.of() : matches.stream()
                // .map(this::isVisible).filter(Objects::nonNull).toList();
                .map(constructor -> new MethodHandleCtorPair<>(isVisible(constructor, lookup), constructor))
                .filter(pair -> pair.handle() != null)
                .toList();

        return matchingHandles.isEmpty() ? null : resolveMostSpecific(matchingHandles);
    }

    private static <T> MethodHandle resolveMostSpecific(List<MethodHandleCtorPair<T>> matches) {
        if (matches.size() == 1) return matches.getFirst().handle();

        Constructor<T> winner = matches.getFirst().ctor();
        MethodHandle winnerHandle = matches.getFirst().handle();
        for (int i = 1; i < matches.size(); i++) {
            MethodHandleCtorPair<T> candidate = matches.get(i);
            Constructor<T> candidateCtor = candidate.ctor();
            if (isMoreSpecific(candidateCtor, winner)) {
                winner = candidateCtor;
                winnerHandle = candidate.handle();
            } else if (!isMoreSpecific(winner, candidateCtor)) {
                // Ambiguity Guard: Ensure winner is strictly more specific than all other matches
                throw new RuntimeException("Ambiguous call: " + winner + " and " + candidateCtor + " are both applicable.");
            }
        }

        return winnerHandle;
    }

    private static boolean isMoreSpecific(Constructor<?> c1, Constructor<?> c2) {
        Class<?>[] p1 = c1.getParameterTypes();
        Class<?>[] p2 = c2.getParameterTypes();

        if (c1.isVarArgs() != c2.isVarArgs()) return !c1.isVarArgs();

        int len = Math.min(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            if (!isPhase1Compatible(p2[i], p1[i])) return false;
        }

        if (c1.isVarArgs()) {
            return isPhase1Compatible(p2[p2.length-1].getComponentType(), p1[p1.length-1].getComponentType());
        }
        return true;
    }

    private static @Nullable MethodHandle isVisible(Constructor<?> ctor, MethodHandles.Lookup lookup) {
        try {
            return lookup.unreflectConstructor(ctor);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    // private static boolean isVisible(@NotNull Constructor<?> ctor, @Nullable Class<?> caller) {
    //     int mod = ctor.getModifiers();
    //     if (Modifier.isPublic(mod)) return true;
    //     if (caller == null) return false;
    //
    //     Class<?> declaring = ctor.getDeclaringClass();
    //     if (getTopLevelClass(declaring) == getTopLevelClass(caller)) return true; // private, protected & default
    //
    //     boolean samePkg = declaring.getPackageName().equals(caller.getPackageName());
    //     if (Modifier.isProtected(mod)) return samePkg || declaring.isAssignableFrom(caller);
    //     return samePkg; // Package-private
    // }

    private static boolean isCompatible(@NotNull Type target, @NotNull TypedClass<?> source, boolean allowBox) {
        Class<?> sourceRaw = source.getTypedClass();

        // Handle Primitives/Boxing first as TypedClass wraps them
        if (target instanceof Class<?> targetClass && targetClass.isPrimitive()) {
            if (sourceRaw == targetClass) return true;
            return WIDENING_PRIMITIVE.getOrDefault(sourceRaw, Collections.emptySet()).contains(targetClass);
        }

        if (allowBox) {
            // Allow TypedClass to handle the heavy lifting for objects and generic bounds
            return source.isAssignableTo(target);
        }

        // Phase 1: Only widening and identity (strict)
        // System.out.println("Getting raw class for type: " + target);
        // System.out.println("Type raw class: " + getRawClass(target));
        // System.out.println("Getting raw class for source: " + source);
        // System.out.println("Source raw class: " + sourceRaw);
        return isPhase1Compatible(getRawClass(target), sourceRaw);
    }

    private static boolean isPhase1Compatible(Class<?> target, Class<?> source) {
        if (target.isAssignableFrom(source)) return true;
        return source.isPrimitive() && target.isPrimitive() &&
                WIDENING_PRIMITIVE.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    @ApiStatus.Internal
    public static Object invoke(MethodHandle handle, IArgument<?>[] args) {
        try {
            Object[] rawArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                rawArgs[i] = args[i].get();
            }

            // todo use spreader
            if (!handle.isVarargsCollector()) return handle.invokeWithArguments(rawArgs);
            int paramCount = handle.type().parameterCount();

            // JLS §15.12.4.2: If the user provided a pre-packed array
            // that matches the varargs type exactly, we must use Fixed Arity. (this is preferred)
            if (args.length == paramCount && args[paramCount-1].getKind() == Argument.Kind.VAR_ARGS) {
                return handle.asFixedArity().invokeWithArguments(rawArgs);
            }

            // Else invokeWithArguments handles the packing. this feature may be removed in future
            return handle.invokeWithArguments(rawArgs);
        } catch (Throwable e) { throw new RuntimeException(e); }
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

    private static <T> List<Constructor<T>> filter(List<Constructor<T>> ctors, IArgument<?>[] args, boolean box, boolean var) {
        TypedClass<?>[] argTypes = Arrays.stream(args)
                .map(arg -> Objects.requireNonNull(arg, "Argument must not be null."))
                .map(IArgument::getStaticType)
                .toArray(TypedClass<?>[]::new);

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
                Type componentType = getVarargComponentType(varargArrayType);
                for (int i = fixedLimit; i < argTypes.length; i++) {
                    Type target = (args[i].getKind() == Argument.Kind.VAR_ARGS) ? varargArrayType : componentType;
                    if (!isCompatible(target, argTypes[i], box)) return false;
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
        // todo use TypedClass#getRawType
        if (type instanceof Class<?> cls) return cls;
        if (type instanceof ParameterizedType pt) return getRawClass(pt.getRawType());
        if (type instanceof GenericArrayType gat) return getRawClass(gat.getGenericComponentType());
        if (type instanceof WildcardType wt) return getRawClass(wt.getUpperBounds()[0]);
        if (type instanceof TypeVariable<?> tv) return getRawClass(tv.getBounds()[0]); // todo we should ensure all bounds conform
        return Object.class; // Fallback for complex captures/variables in Phase 1
    }

    private static Class<?> getTopLevelClass(Class<?> clazz) {
        Class<?> top = clazz;
        while (top.getEnclosingClass() != null) top = top.getEnclosingClass();
        return top;
    }

    @ApiStatus.Internal
    // don't instantiate directly, use factory
    public record CacheKey(Class<?> target, String methodName, Class<?> returnType, List<Class<?>> params, Object effectiveCaller, int lookupModes) {
        public static CacheKey create(Class<?> target, String method, Class<?> returnType, List<Class<?>> params, MethodHandles.Lookup lookup) {
            int modes = lookup.lookupModes();
            Class<?> lookupClass = lookup.lookupClass();
            Object effectiveCaller;

            if ((modes & MethodHandles.Lookup.PRIVATE) != 0) {
                effectiveCaller = lookupClass.getNestHost();
            } else if ((modes & MethodHandles.Lookup.PACKAGE) != 0) {
                effectiveCaller = List.of(lookupClass.getPackageName(), lookupClass.getModule());
            } else if ((modes & MethodHandles.Lookup.MODULE) != 0) {
                effectiveCaller = lookupClass.getModule();
            } else if (modes == MethodHandles.Lookup.PUBLIC || (modes & MethodHandles.Lookup.UNCONDITIONAL) != 0) {
                effectiveCaller = null;
            } else { // fallback
                effectiveCaller = lookupClass;
            }

            return new CacheKey(target, method, returnType, params, effectiveCaller, modes);
        }

        @Override
        public @NotNull String toString() {
            return target.getName() + "#" + methodName + "(" + params.stream().map(Class::getName).collect(Collectors.joining(", ")) + ")" + returnType.getName();
        }
    }
    private record MethodHandleCtorPair<T>(MethodHandle handle, Constructor<T> ctor) {}
}