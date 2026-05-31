package com.github.rcubedev.example.temp;

import org.jetbrains.annotations.NotNull;

public abstract class TypedClassValue<K extends Class<?>, V> {

    private final java.lang.ClassValue<V> delegate = new java.lang.ClassValue<>() {
        @Override
        protected V computeValue(@NotNull Class<?> type) {
            @SuppressWarnings("unchecked")
            K casted = (K) type;
            return TypedClassValue.this.computeValue(casted);
        }
    };

    protected abstract V computeValue(@NotNull K type);

    public final V get(@NotNull K type) {
        return delegate.get(type);
    }

    public final void remove(@NotNull K type) {
        delegate.remove(type);
    }
}