package com.example.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * fixme improve javadoc this is temporary
 * Usage: TypedClass<T> typedClass = new TypedClass<>() {}
 *        Similar to Class<T>, T is the class type followed by generic info.
 *        To hold primitive classes, use {@link #ofPrimitive(Class)} as they
 *        cannot be instantiated generically
 * @param <T> the held class. if primitives are supported, the consumer of this class
 *            should take a TypedClass<?> as primitives cannot match T
 */
public abstract class TypedClass<T> extends TypeReference<T> {

    private static final Map<Class<?>, TypedClass<?>> primitiveInstances = new ConcurrentHashMap<>(); // key and value have same generic sig

    public TypedClass() {
        super();
    }

    // utilised by prim types as they cannot be created generically
    protected TypedClass(Type type) {
        super(type);
    }

    @SuppressWarnings("unchecked")
    public @NotNull Class<T> getTypedClass() {
        Type rawType = getRawType();
        if (rawType instanceof Class<?> clazz) {
            return (Class<T>) clazz;
        }
        throw new RuntimeException("Unexpected type: " + rawType.getTypeName());
    }

    public boolean isAssignableTo(@NotNull TypedClass<?> target) {
        return isAssignableTo(target.getType());
    }

    public boolean isAssignableTo(@NotNull Type target) {
        return isAssignableTo(target, this.getType());
    }

    public static boolean isAssignableTo(@NotNull Type target, @NotNull Type source) {
        if (target.equals(source)) return true;

        return switch (target) {
            case Class<?> targetClass -> targetClass.isAssignableFrom(getRawType(source));

            case ParameterizedType pTarget -> {
                Class<?> rawTarget = (Class<?>) pTarget.getRawType();
                if (!rawTarget.isAssignableFrom(getRawType(source))) yield false;

                if (source instanceof ParameterizedType pSource) {
                    yield compareArguments(pTarget.getActualTypeArguments(), pSource.getActualTypeArguments());
                }
                // Raw type assignment to parameterized type (JLS allows with warning/unchecked)
                yield true;
            }

            case WildcardType wTarget -> isWithinBounds(wTarget, source);

            case TypeVariable<?> tTarget -> {
                for (Type bound : tTarget.getBounds()) {
                    if (!isAssignableTo(bound, source)) yield false;
                }
                yield true;
            }

            case GenericArrayType gaTarget -> {
                Type sourceComponent = getComponentType(source);
                yield sourceComponent != null && isAssignableTo(gaTarget.getGenericComponentType(), sourceComponent);
            }

            default -> target.equals(source);
        };
    }

    private static boolean compareArguments(Type[] targetArgs, Type[] sourceArgs) {
        if (targetArgs.length != sourceArgs.length) return false;
        for (int i = 0; i < targetArgs.length; i++) {
            Type t = targetArgs[i];
            Type s = sourceArgs[i];

            if (t instanceof WildcardType wt) {
                if (!isWithinBounds(wt, s)) return false;
            } else {
                // Invariance: List<String> != List<Object>
                if (!t.equals(s)) return false;
            }
        }
        return true;
    }

    private static boolean isWithinBounds(WildcardType target, Type source) {
        for (Type upper : target.getUpperBounds()) {
            if (!isAssignableTo(upper, source)) return false;
        }
        for (Type lower : target.getLowerBounds()) {
            // Source must be a SUPERTYPE of the lower bound
            if (!isAssignableTo(source, lower)) return false;
        }
        return true;
    }

    private static Type getComponentType(Type type) {
        if (type instanceof Class<?> c) return c.getComponentType();
        if (type instanceof GenericArrayType gat) return gat.getGenericComponentType();
        return null;
    }

    private static Class<?> getRawType(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        if (type instanceof TypeVariable<?> tv) return getRawType(tv.getBounds()[0]);
        if (type instanceof WildcardType wt) return getRawType(wt.getUpperBounds()[0]);
        if (type instanceof GenericArrayType gat) {
            return Array.newInstance(getRawType(gat.getGenericComponentType()), 0).getClass();
        }
        return Object.class;
    }

    /**
     * @param primitiveType
     * @return
     */
    public static <T> TypedClass<T> ofPrimitive(@NotNull Class<T> primitiveType) {
        Objects.requireNonNull(primitiveType, "primitiveType must not be null");
        if (!primitiveType.isPrimitive())
            throw new IllegalArgumentException(primitiveType.getName() + " is not a primitive");
        @SuppressWarnings("unchecked")
        TypedClass<T> typedClass = (TypedClass<T>) primitiveInstances.computeIfAbsent(primitiveType, key -> new PrimitiveTypedClass<>(primitiveType));
        return typedClass;
    }

    /**
     * Use no arg constructor instead
     * @param primitiveArrayType
     * @return
     */
    @ApiStatus.Internal
    public static <T> TypedClass<T> ofPrimitiveArray(@NotNull Class<T> primitiveArrayType) {
        Objects.requireNonNull(primitiveArrayType, "primitiveArrayType must not be null");
        Class<?> componentType = primitiveArrayType.getComponentType();
        if (componentType == null || !componentType.isPrimitive())
            throw new IllegalArgumentException(primitiveArrayType.getSimpleName() + " is not a primitive array type");
        @SuppressWarnings("unchecked")
        TypedClass<T> typedClass = (TypedClass<T>) primitiveInstances.computeIfAbsent(primitiveArrayType, key -> new PrimitiveTypedClass<>(primitiveArrayType));
        return typedClass;
    }

    private final static class PrimitiveTypedClass<T> extends TypedClass<T> {

        public PrimitiveTypedClass(@NotNull Class<T> type) {
            super(type);
            if (!type.isPrimitive() && !type.isArray()) throw new IllegalArgumentException(type.getSimpleName() + " is not a primitive");
            if (type.isArray() && !type.componentType().isPrimitive()) throw new IllegalArgumentException(type.getSimpleName() + " is not a primitive array type");
        }

        @Override
        protected boolean allowRawTypes() { // fixme this class doesnt actually use raw types but uses the type ctor so thinks it does
            return true;
        }
    }
}