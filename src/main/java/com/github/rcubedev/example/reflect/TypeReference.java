package com.github.rcubedev.example.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * fixme add javadoc to this and all stuff most are temporary right now
 * @param <T>
 */
public abstract class TypeReference<T> implements ITypeReference {

    protected final Type type;

    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();

        boolean allowRawTypes = allowRawTypes();
        if (superClass instanceof Class<?> && !allowRawTypes) { // check if rawtypes allowed, if are throw on next check
            throw new IllegalArgumentException("Missing type parameter. Please use: new TypeReference<MyType>() {}");
        }

        if (!(superClass instanceof ParameterizedType)) {
            if (allowRawTypes) {
                throw new IllegalArgumentException("Raw type used with no arg constructor. Use the Type constructor");
            }
            throw new RuntimeException("Unexpected type. Please ensure you are using a valid TypeReference implementation with proper generic type.");
        }

        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    /**
     * Prefer use of {@link TypeReference#TypeReference()}.
     * This should only be used if raw types are allowed and if the type provided is a raw type
     * @param type the type to construct with
     */
    protected TypeReference(@NotNull Type type) {
        if (!allowRawTypes()) throw new IllegalArgumentException("Implementation doesn't allow raw types. If the provided type is not a raw type, use the no arg constructor.");
        this.type = type;
    }

    /**
     * If specifying a generic type is mandatory; if this is true, allow raw types
     * @return if a generic type is mandatory
     */
    protected boolean allowRawTypes() {
        return false;
    }

    public Type getType() { // fixme add another thing for allowing type ctor, and add check to type ctor for rawtypes
        return type;
    }

    public @NotNull Type getRawType() {
        ParameterizedType pType = getParameterizedTypeOrNull();
        if (pType == null) return getType();
        return pType.getRawType();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ITypeReference that)) return false;
        return Objects.equals(getType(), that.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }

    @Override
    public String toString() {
        return getType().getTypeName();
    }

    protected @Nullable ParameterizedType getParameterizedTypeOrNull() {
        if (getType() instanceof ParameterizedType pType) return pType;
        return null;
    }
}

