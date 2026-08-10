package com.github.rcubedev.utils.event.impl.descriptor.method;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Describes a method using JVM type descriptors.
 * <p>
 * Method descriptors are used to determine method identity. Modifiers and
 * other descriptive metadata are intentionally excluded from method identity.
 */
public abstract class MethodDescriptor {

    private final String modifiers;
    private final String declaringClass;
    private final String methodName;
    private final String returnType;
    private final List<String> parameterTypes;

    private String signature;
    private String descriptiveSignature;

    protected MethodDescriptor(@NotNull String modifiers, @NotNull String declaringClass, @NotNull String methodName,
                               @NotNull String returnType, @NotNull List<String> parameterTypes) {
        this.modifiers = Objects.requireNonNull(modifiers);
        this.declaringClass = Objects.requireNonNull(declaringClass);
        this.methodName = Objects.requireNonNull(methodName);
        this.returnType = Objects.requireNonNull(returnType);
        this.parameterTypes = List.copyOf(parameterTypes);
    }

    protected final String modifiers() {
        return this.modifiers;
    }

    public final String declaringClass() {
        return this.declaringClass;
    }

    public final String methodName() {
        return this.methodName;
    }

    public final String returnType() {
        return this.returnType;
    }

    public final List<String> parameterTypes() {
        return this.parameterTypes;
    }

    /**
     * Generates the JVM-oriented method signature.
     * <p>
     * This value includes the declaring type, method name, parameter descriptors,
     * and return descriptor.
     * <p>
     * <b>Example:</b> {@code Ljava/util/List;set(ILjava/lang/Object;)ILjava/lang/Object;}
     *
     * @apiNote This value is derived entirely from the fields used by {@link #equals(Object)}
     *          and {@link #hashCode()}, and is therefore identical between equal
     *          {@link MethodDescriptor} instances.
     */
    public final String getDescriptiveSignature() {
        String result = this.descriptiveSignature;
        if (result == null) {
            result = declaringClass() + methodName() +
                    "(" + String.join("", parameterTypes()) + ")" + returnType();
            this.descriptiveSignature = result;
        }
        return result;
    }

    /**
     * Generates a human-readable method signature, including available modifiers.
     * <p>
     * The signature is formatted as:
     * {@code [modifiers ]returnType declaringClass.methodName(parameterTypes)}
     * <p>
     * Parameter types are comma-separated with no whitespace after the commas.
     * When no modifiers are available, the signature begins directly with the
     * return type.
     * <p>
     * <b>Example:</b> {@code public java.lang.Object java.util.List.set(int,java.lang.Object)}
     *
     * @return a human-readable representation of this method
     * @apiNote Modifiers are metadata and are not considered by {@link #equals(Object)} or {@link #hashCode()}.
     * @implSpec Implementations may override this method to provide a more
     *           efficient representation, but must preserve the format specified
     *           by this method.
     */
    public String getSignature() {
        String result = this.signature;
        if (result == null) {
            String prefix = modifiers.isEmpty() ? "" : modifiers + " ";

            result = prefix + simpleType(returnType()) + " " + simpleType(declaringClass()) + "." + methodName() +
                    parameterTypes().stream().map(MethodDescriptor::simpleType)
                            .collect(Collectors.joining(",", "(", ")"));
            this.signature = result;
        }
        return result;
    }

    protected static String simpleType(String descriptor) {
        int dimensions = 0;

        while (descriptor.charAt(dimensions) == '[') {
            dimensions++;
        }

        String type = switch (descriptor.charAt(dimensions)) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'C' -> "char";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'V' -> "void";

            case 'L' -> descriptor.substring(
                    dimensions + 1,
                    descriptor.length() - 1
            ).replace('/', '.');

            default -> throw new IllegalArgumentException(
                    "Invalid type descriptor: " + descriptor
            );
        };

        return type + "[]".repeat(dimensions);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote Equality is based solely on the declaring class, method name,
     *          return type, and parameter types. Descriptive metadata such as
     *          modifiers are intentionally excluded.
     *          <p>
     *          Two equal {@link MethodDescriptor} instances therefore have identical
     *          {@link #getDescriptiveSignature()} values, but are not required to have
     *          identical {@link #getSignature()} values.
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MethodDescriptor other)) {
            return false;
        }

        return Objects.equals(this.declaringClass, other.declaringClass)
                && Objects.equals(this.methodName, other.methodName)
                && Objects.equals(this.returnType, other.returnType)
                && Objects.equals(this.parameterTypes, other.parameterTypes);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote The hash code is derived solely from the declaring class, method
     *          name, return type, and parameter types. Descriptive metadata such as
     *          modifiers is intentionally excluded.
     *          <p>
     *          This is consistent with {@link #equals(Object)}.
     */
    @Override
    public final int hashCode() {
        return Objects.hash(declaringClass, methodName, returnType, parameterTypes);
    }

    @Override
    public final String toString() {
        return this.getSignature();
    }
}