package com.github.rcubedev.example;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OldReflectionHelper {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = Map.of(
            int.class, Integer.class,
            long.class, Long.class,
            boolean.class, Boolean.class,
            double.class, Double.class,
            float.class, Float.class,
            char.class, Character.class,
            byte.class, Byte.class,
            short.class, Short.class
    );

    private OldReflectionHelper() {}

    // Cache to prevent re-calculating the hierarchy tree repeatedly
    private static final Map<@NotNull TypePair, @NotNull Integer> DISTANCE_CACHE = new ConcurrentHashMap<>();

    /**
     * Instantiates a class using its zero-argument constructor.
     *
     * @param clazz the class to instantiate
     * @param <T> the type of the object
     * @param <R> the return type (supertype of {@link T})
     * @return a new instance of {@link T}, of type {@link R}
     * @throws IllegalStateException if no zero-argument constructor is found
     * @throws RuntimeException      if the constructor invocation fails
     */
    public static <T extends R, R> @NotNull R instantiate(@NotNull Class<T> clazz) {
        return instantiate(clazz, new Object[0]);
    }

    /**
     * Finds the best-matching public constructor for the given class and arguments, and instantiates it.
     *
     * @param clazz the class to instantiate
     * @param args the constructor arguments
     * @param <T> the type of the object
     * @param <R> the return type (supertype of {@link T})
     * @return a new instance of {@link T}, of type {@link R}
     * @throws IllegalStateException if no matching constructor is found
     */
    public static <T extends R, R> @NotNull R instantiate(@NotNull Class<T> clazz, Object @Nullable ... args) {
        Objects.requireNonNull(args, "The arguments must not be null. Use the no-args overload instead.");

        /*
        List<Constructor<T>> matchingCtors = new ArrayList<>();
        int bestDistance = Integer.MAX_VALUE;

        for (Constructor<?> _ctor : clazz.getConstructors()) {
            @SuppressWarnings("unchecked")
            Constructor<T> ctor = (Constructor<T>) _ctor;
            Class<?>[] paramTypes = ctor.getParameterTypes();

            if (!parametersMatch(paramTypes, args)) continue;

            int currentCtorDistance = 0;
            for (int i = 0; i < paramTypes.length; i++) {
                Object arg = args[i];
                if (arg == null) continue; // skip distance computation for null as type is unknown

                int paramDistance = DISTANCE_CACHE.computeIfAbsent(
                        new TypePair(arg.getClass(), wrapPrimitiveOrSame(paramTypes[i])),
                        pair -> findTypeDistance(pair.start(), pair.target())
                );

                if (paramDistance == Integer.MAX_VALUE) {
                    // Safeguard: this shouldn't ever run; instead handled by parametersMatch check
                    throw new IllegalStateException("Argument " + arg.getClass().getName() +
                            " is not assignable to parameter type " + paramTypes[i].getName());
                }

                if (currentCtorDistance > Integer.MAX_VALUE - paramDistance) currentCtorDistance = Integer.MAX_VALUE;
                else currentCtorDistance += paramDistance;

                // Exit early; this constructor cannot beat the current best
                if (currentCtorDistance > bestDistance) break;
            }

            if (currentCtorDistance > bestDistance) continue;
            if (currentCtorDistance < bestDistance) {
                bestDistance = currentCtorDistance;
                matchingCtors.clear();
            }
            matchingCtors.add(ctor);
        }

        if (!matchingCtors.isEmpty()) {
            // Parameter count (fewer = preferred), then by class name lex
            matchingCtors.sort(Comparator
                    .<Constructor<T>>comparingInt(Constructor::getParameterCount)
                    .thenComparing(c -> Arrays.toString(Arrays.stream(c.getParameterTypes())
                            .map(Class::getName).toArray(String[]::new)))
            );

            Constructor<T> bestCtor = matchingCtors.getFirst();
            try {
                return bestCtor.newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke constructor " + bestCtor, e);
            }
        }

        String argsStr = Arrays.stream(args)
                .map(arg -> arg == null ? "any type" : arg.getClass().getSimpleName())
                .collect(Collectors.joining(", "));

        throw new IllegalStateException("No matching constructor found for " + clazz.getSimpleName() + "(" + argsStr + ")");
        */
        Constructor<T> ctor = findConstructor(clazz, args);

        if (ctor == null) {
            String argsStr = Arrays.stream(args)
                    .map(arg -> arg == null ? "any type" : arg.getClass().getSimpleName())
                    .collect(Collectors.joining(", "));

            throw new IllegalStateException("No matching constructor found for " + clazz.getSimpleName() + "(" + argsStr + ")");
        }
        try {
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke constructor " + ctor, e);
        }
    }

    /**
     * Finds the zero-argument public constructor for the given class.
     *
     * @param clazz the class to search
     * @param <T> the type of the class
     * @return the zero-argument {@link Constructor}, or {@code null} if none match
     */
    public static <T> @Nullable Constructor<T> findConstructor(@NotNull Class<T> clazz) {
        return findConstructor(clazz, new Object[0]);
    }

    /**
     * Finds the single best-matching public constructor for the given class and arguments.
     * <p>
     * If multiple constructors have the same "distance" score, they are sorted lowest to
     * highest by parameter count and then lexicographically by parameter type names to
     * ensure deterministic selection.
     *
     * @param clazz the class to search
     * @param args the arguments to be passed to the constructor
     * @param <T> the type of the class
     * @return the best-matching {@link Constructor}, or {@code null} if none match
     * @throws NullPointerException if the args array is null
     */
    public static <T> @Nullable Constructor<T> findConstructor(@NotNull Class<T> clazz, Object @Nullable [] args) {
        Objects.requireNonNull(args, "The arguments array must not be null. Use the no-args overload instead.");

        List<Constructor<T>> constructors = findConstructors(clazz, args);
        if (constructors.isEmpty()) return null;
        if (constructors.size() == 1) return constructors.getFirst();

        constructors.sort(Comparator
                .<Constructor<T>>comparingInt(Constructor::getParameterCount)
                .thenComparing(c -> Arrays.toString(Arrays.stream(c.getParameterTypes())
                        .map(Class::getName).toArray(String[]::new)))
        );
        return constructors.getFirst();
    }

    /**
     * Filters all public constructors of a class that are compatible with the provided arguments,
     * returning only those with the lowest inheritance distance score.
     *
     * @param clazz the class to search
     * @param args the arguments to match against
     * @param <T> the type of the class
     * @return a list of best-matching {@link Constructor} candidates
     * @throws NullPointerException if the args array is null
     */
    public static <T> @NotNull List<Constructor<T>> findConstructors(@NotNull Class<T> clazz, Object @Nullable [] args) {
        Objects.requireNonNull(args, "The arguments array must not be null");

        List<Constructor<T>> ctorCandidates = new ArrayList<>();

        if (args.length == 0) {
            try {
                return List.of(clazz.getConstructor());
            } catch (NoSuchMethodException e) {
                // Fall through to the loop logic to see if a varargs ctor matches
            }
        }

        for (Constructor<?> ctor : clazz.getConstructors()) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            if (parametersMatch(paramTypes, args)) {
                @SuppressWarnings("unchecked")
                Constructor<T> tCtor = (Constructor<T>) ctor;
                ctorCandidates.add(tCtor);
            }
        }

        if (ctorCandidates.isEmpty() || ctorCandidates.size() == 1) return ctorCandidates;

        List<Constructor<T>> bestCtors = new ArrayList<>();
        int bestDistance = Integer.MAX_VALUE;
        for (Constructor<T> ctor : ctorCandidates) {
            Class<?>[] paramTypes = ctor.getParameterTypes();

            int distance = computeTotalDistance(paramTypes, args, bestDistance);

            if (distance > bestDistance) continue;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestCtors.clear();
            }
            bestCtors.add(ctor);
        }
        return bestCtors;
    }

    /**
     * Computes the total inheritance distance for a set of parameter types against
     * a set of provided arguments.
     * <p>
     * This method normalizes primitive parameter types using {@link #wrapPrimitive(Class)}
     * before delegating to {@link #findTypeDistance(Class, Class)}.
     * <p>
     * If the running sum of distances exceeds {@code bestSoFar}, computation
     * terminates early to optimize constructor selection.
     *
     * @param paramTypes the types defined in the constructor signature
     * @param args the actual argument objects provided
     * @param bestSoFar the current minimum distance found in other candidates
     * @return the total distance score, or {@link Integer#MAX_VALUE} if incompatible
     * @throws NullPointerException if the args array is null
     */
    public static int computeTotalDistance(Class<?> @NotNull [] paramTypes, Object @Nullable [] args, int bestSoFar) {
        Objects.requireNonNull(args, "The arguments array must not be null");
        int totalDistance = 0;

        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) continue; // skip distance computation for null as type is unknown

            int paramDistance = findTypeDistance(arg.getClass(), wrapPrimitive(paramTypes[i]));
            if (paramDistance == Integer.MAX_VALUE) {
                // // Safeguard: this shouldn't ever run; instead handled by parametersMatch check
                // throw new IllegalStateException("Argument " + arg.getClass().getName() +
                //         " is not assignable to parameter type " + paramTypes[i].getName());
                return Integer.MAX_VALUE;
            }

            if (totalDistance > Integer.MAX_VALUE - paramDistance) totalDistance = Integer.MAX_VALUE;
            else totalDistance += paramDistance;

            // Exit early; this constructor cannot beat the current best
            if (totalDistance > bestSoFar) break;
        }
        return totalDistance;
    }

    /**
     * Computes the minimum inheritance distance between two types.
     * <p>
     * The distance is defined as the length of the shortest path from {@code start}
     * to {@code target} when traversing the type hierarchy (superclasses and interfaces).
     * Traversal is performed using breadth-first search.
     * <p>
     * Note: This method is agnostic to Java primitives. For accurate distance
     * scoring against primitive parameters, ensure the {@code target} class is
     * boxed via {@link #wrapPrimitive(Class)} before calling this method.
     * <p>
     * A distance of {@code 0} indicates the two types are identical.
     * A distance of {@code 1} indicates a direct superclass or directly implemented interface.
     * <p>
     * If {@code target} is not reachable from {@code start},
     * {@link Integer#MAX_VALUE} is returned.
     *
     * @param start the starting class or interface
     * @param target the type whose distance from {@code start} is measured
     * @return the minimum distance, or {@link Integer#MAX_VALUE} if unreachable
     */
    public static int findTypeDistance(@NotNull Class<?> start, @NotNull Class<?> target) {
        if (start.equals(target)) return 0;

        return DISTANCE_CACHE.computeIfAbsent(new TypePair(start, target), pair -> {
            Queue<Class<?>> queue = new ArrayDeque<>();
            Map<Class<?>, Integer> distance = new HashMap<>();

            queue.add(start);
            distance.put(start, 0);

            while (!queue.isEmpty()) {
                Class<?> current = queue.poll();
                int d = distance.get(current);
                if (current.equals(target)) return d;

                Class<?> superClass = current.getSuperclass();
                if (superClass != null) {
                    if (superClass.equals(target)) return d + 1; // Early exit
                    if (!distance.containsKey(superClass)) {
                        distance.put(superClass, d + 1);
                        queue.add(superClass);
                    }
                }

                for (Class<?> iface : current.getInterfaces()) {
                    if (iface.equals(target)) return d + 1; // Early exit
                    if (!distance.containsKey(iface)) {
                        distance.put(iface, d + 1);
                        queue.add(iface);
                    }
                }
            }

            return Integer.MAX_VALUE;
        });
    }

    /**
     * Checks if the given constructor parameter types are compatible with the provided arguments.
     * <p>
     * This performs a basic compatibility check (length match and {@link Class#isAssignableFrom}).
     * Primitive types are automatically wrapped for the check. Null arguments are considered
     * compatible with any non-primitive parameter type.
     *
     * @param paramTypes the constructor parameter types
     * @param args the arguments to match
     * @return true if all arguments are compatible with their corresponding parameter types
     */
    public static boolean parametersMatch(Class<?> @NotNull [] paramTypes, Object @Nullable [] args) {
        Objects.requireNonNull(args, "The arguments array must not be null");
        if (paramTypes.length != args.length) return false;

        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            Class<?> param = wrapPrimitive(paramTypes[i]);

            if (arg == null) {
                if (paramTypes[i].isPrimitive()) return false;
            } else if (!param.isAssignableFrom(arg.getClass())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a primitive class type to its boxed equivalent.
     * Returns the original class if it is not primitive
     *
     * @param clazz the class to wrap
     * @return boxed class equivalent if primitive, else the same class
     */
    public static @NotNull Class<?> wrapPrimitive(@NotNull Class<?> clazz) {
        if (!clazz.isPrimitive()) return clazz;
        Class<?> wrapped = PRIMITIVE_WRAPPERS.get(clazz);
        if (wrapped == null) throw new IllegalStateException("Unknown primitive type: " + clazz);
        return wrapped;
    }
}