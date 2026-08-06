package com.github.rcubedev.utils.config.serialization;

import java.lang.invoke.MethodHandles;
import java.util.NoSuchElementException;

import com.github.rcubedev.utils.config.AllowConstructorFallback;
import com.github.rcubedev.utils.reflect.Argument;
import com.github.rcubedev.utils.reflect.IArgument;
import com.github.rcubedev.utils.reflect.JlsReflectionHelper;
import com.github.rcubedev.utils.reflect.TypedClass;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ComplexConfigValue;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for objects in the config system that can store either an element of type {@link A} or an element of type {@link B}.
 *
 * @implSpec Subclasses must provide constructors matching the signatures required by {@link #newInstanceOfA(A)} and {@link #newInstanceOfB(B)}
 * @param <A> the potential element type
 * @param <B> the potential element type
 * @param <S> the subclass type
 */
public abstract class AbstractDualElementValue<A, B, S extends AbstractDualElementValue<A, B, S>> implements ConfigSerializableObject<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger("Kaleido Config"); // todo maybe change

    private final Object value; // Can hold either A or B
    private final TypedClass<A> typeA;
    private final TypedClass<B> typeB;
    private final boolean isA;

    /**
     * Initialises the element
     *
     * @param value the value to store; an instance of either {@link A} or {@link B}
     * @param typeA the {@link TypedClass} object of {@link A}
     * @param typeB the {@link TypedClass} object of {@link B}
     * @param isA if {@code value} is {@link A} or {@link B}
     * @throws IllegalArgumentException if {@code value} is not an instance of A (isA == true) or B (isA == false)
     */
    public <T> AbstractDualElementValue(T value, TypedClass<A> typeA, TypedClass<B> typeB, boolean isA) {
        TypedClass<?> type = isA ? typeA : typeB;
        if (!type.getTypedClass().isInstance(value)) throw new IllegalArgumentException("value is not an instance of " + type);
        this.value = value;
        this.typeA = typeA;
        this.typeB = typeB;
        this.isA = isA;
    }

    public boolean isA() {
        return this.isA;
    }

    public final boolean isB() {
        return !isA();
    }

    /**
     * Returns the {@link TypedClass}.
     *
     * @return the type of the element.
     */
    protected TypedClass<?> getType() {
        return isA() ? this.typeA : this.typeB;
    }

    protected TypedClass<A> getTypeA() {
        return this.typeA;
    }

    protected TypedClass<B> getTypeB() {
        return this.typeB;
    }

    /**
     * Creates a new instance of {@link S}.
     * Subclasses must have a matching constructor.
     *
     * @param value the single value to pass to the constructor
     * @return a new instance of {@link S}
     * @throws IllegalStateException if no matching constructor exists
     * @implSpec This method uses reflection to create a new instance of the runtime subclass {@link S}.
     *           The subclass must have a constructor accepting {@link A} else {@link IllegalStateException} is thrown.
     */
    protected @NotNull S newInstanceOfA(A value) {
        TypedClass<A> typeA = getTypeA();
        try {
            return invokeMatchingConstructor(Argument.Builder.of(value, typeA));
        } catch (NoSuchElementException firstEx) { // fixme change when the ex is changed
            try {
                logFallback(this.getClass().getTypeName(), value.getClass().getTypeName(), typeA.getClass().getGenericSuperclass().getTypeName());
                return invokeMatchingConstructor(Argument.Builder.of(value, typeA), Argument.Builder.of(typeA, new TypedClass<>(){}), Argument.Builder.of(getTypeB(), new TypedClass<>(){}));
            } catch (NoSuchElementException fallbackEx) {
                if (allowCtorFallback()) {
                    // Fallback allowed, throw the fallback (with firstEx suppressed)
                    fallbackEx.addSuppressed(firstEx);
                    throw fallbackEx;
                } else {
                    // Fallback not allowed, throw the firstEx (with fallback suppressed)
                    firstEx.addSuppressed(fallbackEx);
                    throw firstEx;
                }
            }
        }
    }

    /**
     * Creates a new instance of {@link S}.
     * Subclasses must have a matching constructor.
     *
     * @param value the collection of values to pass to the constructor
     * @return a new instance of {@link S}
     * @throws IllegalStateException if no matching constructor exists
     * @implSpec This method uses reflection to create a new instance of the runtime subclass {@link S}.
     *           The subclass must have a constructor accepting {@link B} else {@link IllegalStateException} is thrown.
     */
    protected @NotNull S newInstanceOfB(B value) {
        TypedClass<B> typeB = getTypeB();
        try {
            return invokeMatchingConstructor(Argument.Builder.of(value, typeB));
        } catch (NoSuchElementException firstEx) { // fixme change when the ex is changed
            try {
                logFallback(this.getClass().getTypeName(), value.getClass().getTypeName(), typeB.getClass().getGenericSuperclass().getTypeName());
                return invokeMatchingConstructor(Argument.Builder.of(value, typeB), Argument.Builder.of(getTypeA(), new TypedClass<>(){}), Argument.Builder.of(typeB, new TypedClass<>(){}));
            } catch (NoSuchElementException fallbackEx) {
                if (allowCtorFallback()) {
                    // Fallback allowed, throw the fallback (with firstEx suppressed)
                    fallbackEx.addSuppressed(firstEx);
                    throw fallbackEx;
                } else {
                    // Fallback not allowed, throw the firstEx (with fallback suppressed)
                    firstEx.addSuppressed(fallbackEx);
                    throw firstEx;
                }
            }
        }
    }

    /**
     * Attempts to instantiate the runtime subclass using the best-matching public
     * constructor for the given arguments.
     * <p>
     * Constructor selection prefers the closest match in the class hierarchy
     * (exact matches over superclasses). Primitive parameters are matched against
     * their boxed equivalents.
     *
     * @param args the constructor arguments
     * @return a new instance of {@link S}
     * @throws IllegalStateException if no compatible constructor exists
     * @throws RuntimeException if constructor invocation fails
     */
    @SuppressWarnings("unchecked")
    protected final @NotNull S invokeMatchingConstructor(IArgument<?>... args) {
        return JlsReflectionHelper.getInstance((Class<? extends S>) this.getClass(), MethodHandles.publicLookup()).instantiate(args);
        // return OldReflectionHelper.instantiate((Class<? extends S>) this.getClass(), args);
    }

    /**
     * Determines whether this class explicitly allows reflective constructor
     * fallback without logging a warning.
     * <p>
     * Classes annotated with {@link AllowConstructorFallback} signal that using
     * a fallback constructor is intentional and should not be warned about.
     *
     * @return {@code true} if fallback constructor usage is allowed
     */
    protected boolean allowCtorFallback() {
        return this.getClass().isAnnotationPresent(AllowConstructorFallback.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public S convertFrom(Object representation) {
        Class<A> classA = getTypeA().getTypedClass();
        Class<B> classB = getTypeB().getTypedClass();

        if (classA.isInstance(representation)) {
            return newInstanceOfA((A) representation);
        }
        if (classB.isInstance(representation)) {
            return newInstanceOfB((B) representation);
        }
        throw new IllegalArgumentException("Invalid representation: " + representation);
    }

    @Override
    public Object getRepresentation() {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S copy() {
        if (value instanceof ComplexConfigValue complexConfigValue) {
            return isA() ? newInstanceOfA((A) complexConfigValue.copy()) : newInstanceOfB((B) complexConfigValue.copy());
        }
        return isA() ? newInstanceOfA((A) value) : newInstanceOfB((B) value);
    }

    @Override
    public String toString() {
        Object representation = getRepresentation();
        // if (representation instanceof Collection<?> collection) return this.getClass().getName() + "=" + collectionToString(collection);
        return this.getClass().getName() + "=" + representation.toString();
        // throw new IllegalArgumentException("Invalid representation: " + representation);
    }

    /**
     * Logs a warning when a reflective constructor fallback is used,
     * unless the class is annotated with {@link AllowConstructorFallback}.
     *
     * @param classCtor the class whose constructor is being invoked
     * @param ctorArgs the constructor argument types
     */
    private void logFallback(String classCtor, String @NotNull ... ctorArgs) {
        if (allowCtorFallback()) return;

        String argsStr = String.join(", ", ctorArgs);

        // fixme use ILoggerRequired
        LOGGER.warn("Falling back to constructor '{}({})'. This is supported but not recommended; prefer defining an explicit constructor.",
                classCtor, argsStr);
    }
}
