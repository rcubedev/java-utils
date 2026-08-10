package com.github.rcubedev.utils.event.impl.subscriber;

import com.github.rcubedev.utils.event.impl.descriptor.method.MethodKeyDescriptor;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public final class MethodKey {

    private final Identity identity;
    private final String modifiers;

    public MethodKey(Class<?> clazz, String methodName, MethodType type, @Nullable String modifiers) {
        this.identity = new Identity(clazz, methodName, type);
        this.modifiers = modifiers == null || modifiers.isBlank() ? "" : modifiers;
    }

    public MethodKey(Method method) {
        this(method.getDeclaringClass(), method.getName(),
                MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                Modifier.toString(method.getModifiers()));
    }

    public MethodKey(Class<?> clazz, String methodName, MethodType type) {
        this(clazz, methodName, type, null);
    }

    public Class<?> clazz() {
        return this.identity.clazz();
    }

    public String methodName() {
        return this.identity.methodName;
    }

    public MethodType type() {
        return this.identity.type;
    }

    /**
     * Returns the method's access modifiers and other modifier metadata.
     * <p>
     * This value is normalised to an empty string when modifiers are unavailable
     * or blank.
     *
     * @return the method's modifiers
     * @apiNote Modifiers are descriptive metadata and are not considered part of the
     *          method's identity.
     */
    public String modifiers() {
        return this.modifiers;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote Equality is based solely on the declaring class, method name,
     *          and {@link MethodType}. Descriptive metadata such as modifiers is
     *          intentionally excluded.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MethodKey other)) return false;

        return this.identity.equals(other.identity);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote The hash code is based solely on the declaring class, method name,
     *          and {@link MethodType}. Descriptive metadata such as modifiers is
     *          intentionally excluded.
     *          <p>
     *          This is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return this.identity.hashCode();
    }

    @Override
    public String toString() {
        return MethodKeyDescriptor.of(this).getSignature();
    }

    /**
     * Represents the portion of a method key that determines method identity.
     *
     * @param clazz the declaring class of the method
     * @param methodName the method name
     * @param type the {@link MethodType} of the method
     */
    private record Identity(Class<?> clazz, String methodName, MethodType type) {

        private Identity {
            Objects.requireNonNull(clazz);
            Objects.requireNonNull(methodName);
            Objects.requireNonNull(type);
        }
    }
}
