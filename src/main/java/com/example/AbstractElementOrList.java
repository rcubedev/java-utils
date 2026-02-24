package com.example;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import java.util.NoSuchElementException;

import com.example.reflect.Argument;
import com.example.reflect.IArgument;
import com.example.reflect.JlsReflectionHelper;
import com.example.reflect.TypedClass;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ComplexConfigValue;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for objects in the config system that can store either a single element of type {@link T} or a collection of elements.
 *
 * @implSpec Subclasses must provide constructors matching the signatures required by {@link #newInstance(T)} and {@link #newInstance(List)}
 * @param <T> the element type
 * @param <S> the subclass type
 */
public abstract class AbstractElementOrList<T, S extends AbstractElementOrList<T, S>> implements ConfigSerializableObject<Object> {

    private final Object value; // Can hold either T or a ValueList<T>
    private final TypedClass<T> type;

    /**
     * Single-value constructor.
     * Initializes the element with a single value of type {@link T}.
     *
     * @param value the value to store
     * @param type the Class object of {@link T}
     */
    public AbstractElementOrList(T value, TypedClass<T> type) {
        this.value = value;
        this.type = type;
    }

    /**
     * Collection constructor.
     * Initializes the element with a list of values of type {@link T}.
     *
     * @param values the list of values to store
     * @param type the Class object of {@link T}
     */
    @SuppressWarnings("unchecked")
    public AbstractElementOrList(List<@NotNull T> values, TypedClass<T> type) {
        this(values.toArray((T[]) Array.newInstance(type.getTypedClass(), 0)), type);
    }

    // Internal ctor
    // private AbstractElementOrList(T @NotNull [] values, Class<T> type) {
    //     if (values.length == 0) throw new IllegalArgumentException("values must not be empty, first element must be a minimal object to identify the type");
    //     this.value = ValueList.create(values[0], values);
    //     this.type = type;
    // }
    // fixme is this proper? the minimal obj appears to be going into the create method twice; i think it is as i dont think we take a minimal obj.
    private AbstractElementOrList(T @NotNull [] values, TypedClass<T> type) {
        if (values.length == 0) throw new IllegalArgumentException("values must not be empty");
        // T[] safeArray = Arrays.copyOf(values, values.length, (Class<? extends T[]>) Array.newInstance(type.getTypedClass(), values.length).getClass());
        T[] safeArray = Arrays.copyOf(values, values.length);
        this.value = ValueList.create(safeArray[0], safeArray); // safe, mutable list
        this.type = type;
    }


    /**
     * Returns the Class object of {@link T}.
     *
     * @return the type of the elements
     */
    protected TypedClass<T> getType() {
        return this.type;
    }

    /**
     * Creates a new instance of {@link S} using the single-value constructor.
     * Subclasses must have a matching constructor.
     *
     * @param value the single value to pass to the constructor
     * @return a new instance of {@link S}
     * @throws IllegalStateException if no matching constructor exists
     * @implSpec This method uses reflection to create a new instance of the runtime subclass {@link S}.
     *           The subclass must have a constructor accepting {@link T} else {@link IllegalStateException} is thrown.
     */
    protected @NotNull S newInstance(T value) {
        try {
            return invokeMatchingConstructor(Argument.Builder.of(value, type));
        } catch (NoSuchElementException firstEx) { // fixme change when the ex is changed
            try {
                logFallback(this.getClass().getTypeName(), value.getClass().getTypeName(), type.getClass().getGenericSuperclass().getTypeName());
                return invokeMatchingConstructor(Argument.Builder.of(value, type), Argument.Builder.of(getType(), new TypedClass<>(){}));
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
     * Creates a new instance of {@link S} using the collection constructor.
     * Subclasses must have a matching constructor.
     *
     * @param values the collection of values to pass to the constructor
     * @return a new instance of {@link S}
     * @throws IllegalStateException if no matching constructor exists
     * @implSpec This method uses reflection to create a new instance of the runtime subclass {@link S}.
     *           The subclass must have a constructor accepting {@link List<T>} else {@link IllegalStateException} is thrown.
     */
    protected @NotNull S newInstance(List<T> values) {
        try {
            return invokeMatchingConstructor(Argument.Builder.of(values, new TypedClass<>(){}));
        } catch (NoSuchElementException firstEx) { // fixme change when the ex is changed
            try {
                logFallback(this.getClass().getTypeName(), List.class.getTypeName(), type.getClass().getGenericSuperclass().getTypeName());
                return invokeMatchingConstructor(Argument.Builder.of(values, new TypedClass<>(){}), Argument.Builder.of(getType(), new TypedClass<>(){}));
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

    @Override
    public S convertFrom(Object representation) {
        Class<T> clazz = getType().getTypedClass();

        if (clazz.isInstance(representation)) {
            return newInstance((T) representation);
        }

        // // fixme not sure why i convert to string here, this also causes unchecked cast as e.g. if representation is an Integer it will get converted to string
        // // and then be casted back to integer which is unchecked and would throw CCE
        // String strRepresentation = StringHelper.toStringExcludingNull(representation);
        // if (clazz.isInstance(strRepresentation)) return newInstance((T) strRepresentation);

        if (representation instanceof List<?> list) {
            // increased safety if T is non-final, don't just cast the list
            if (list.stream().allMatch(clazz::isInstance)) {
                List<T> typedList = list.stream().map(clazz::cast).toList();
                return newInstance(typedList);
            }

            // code smell; then you should also allow string -> prim, etc.
            // // If T is String, be more lenient by allowing primitives
            // if (clazz == String.class) {
            //     List<T> strList = new ArrayList<>();
            //     for (Object item : list) {
            //         String itemString = StringHelper.toStringExcludingNull(item);
            //
            //         // Handle boxed primitives and null-safe toString conversion
            //         if (itemString == null) {
            //             throw new IllegalArgumentException("ValueList contains non stringable elements: " + representation);
            //         } else {
            //             strList.add((T) itemString);
            //         }
            //     }
            //
            //     return newInstance(strList);
            // }
        }
        throw new IllegalArgumentException("Invalid representation: " + representation);
    }

    @Override
    public Object getRepresentation() {
        return value;
    }

    @Override
    public S copy() {
        if (value instanceof ComplexConfigValue valueList) {
            return newInstance((ValueList<T>) valueList.copy()); // safe cast as constuctors set value as ValueList<T>
        }
        if (value instanceof ConfigSerializableObject configValue) {
            return newInstance((T) configValue.copy());
        }
        return newInstance((T) value);
    }

    @Override
    public String toString() {
        Object representation = getRepresentation();
        String strRepresentation = StringHelper.toString(representation);
        if (strRepresentation != null) return strRepresentation;
        else if (representation instanceof Collection<?> collection) return collectionToString(collection);
        throw new IllegalArgumentException("Invalid representation: " + representation);
    }

    // TODO :: Make Collection util class
    // Taken from AbstractCollection, used as ValueList doesn't extend AbstractCollection/AbstractList leading to bad output
    public <E> String collectionToString(Collection<E> list) {
        Iterator<E> it = list.iterator();
        if (!it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            E e = it.next();
            sb.append(e == list ? "(this Collection)" : e);
            if (! it.hasNext())
                return sb.append(']').toString();
            sb.append(',').append(' ');
        }
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

        ExampleMod.LOGGER.warn("(Config) Falling back to constructor '{}({})'. This is supported but not recommended; prefer defining an explicit constructor.",
                classCtor, argsStr);
    }
}

