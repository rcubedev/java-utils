package com.github.rcubedev.utils.config.serialization;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.github.rcubedev.utils.reflect.TypedClass;
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
public abstract class AbstractElementOrList<T, S extends AbstractElementOrList<T, S>> extends AbstractDualElementValue<T, List<T>, S> implements ConfigSerializableObject<Object> {

    /**
     * Single-value constructor.
     * Initializes the element with a single value of type {@link T}.
     *
     * @param value the value to store
     * @param type the Class object of {@link T}
     */
    public AbstractElementOrList(T value, TypedClass<T> type, TypedClass<List<T>> listType) {
        super(value, type, listType, true);
    }

    /**
     * Collection constructor.
     * Initializes the element with a list of values of type {@link T}.
     *
     * @param values the list of values to store
     * @param type the Class object of {@link T}
     */
    @SuppressWarnings("unchecked")
    public AbstractElementOrList(List<@NotNull T> values, TypedClass<T> type, TypedClass<List<T>> listType) {
        this(values.toArray((T[]) Array.newInstance(type.getTypedClass(), values.size())), type, listType);
    }

    // fixme maybe allow values to be empty actually to create empty lists or something like [].
    private AbstractElementOrList(T @NotNull [] values, TypedClass<T> type, TypedClass<List<T>> listType) {
        super(passValues(values, type), type, listType, false);
    }

    // needed as super ctor must be first
    private static <T> ValueList<T> passValues(T[] values, TypedClass<T> type) {
        if (values.length == 0) throw new IllegalArgumentException("values must not be empty");
        return ValueList.create(values[0], Arrays.copyOf(values, values.length, (Class<? extends T[]>) Array.newInstance(type.getTypedClass(), values.length).getClass()));
    }

    /**
     * Returns the Class object of {@link T}.
     *
     * @return the type of the element(s)
     */
    @Override
    protected TypedClass<T> getType() {
        return getTypeA();
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
        return newInstanceOfA(value);
    }

    /**
     * Creates a new instance of {@link S} using the list constructor.
     * Subclasses must have a matching constructor.
     *
     * @param values the list of values to pass to the constructor
     * @return a new instance of {@link S}
     * @throws IllegalStateException if no matching constructor exists
     * @implSpec This method uses reflection to create a new instance of the runtime subclass {@link S}.
     *           The subclass must have a constructor accepting {@link List List&ltT&gt} else {@link IllegalStateException} is thrown.
     */
    protected @NotNull S newInstance(List<T> values) {
        return newInstanceOfB(values);
    }

    @Override
    public S convertFrom(Object representation) {
        if (representation instanceof List<?> list) {
            // increased safety if T is non-final, don't just cast the list
            Class<T> clazz = getType().getTypedClass();
            if (list.stream().allMatch(clazz::isInstance)) {
                List<T> typedList = list.stream().map(clazz::cast).toList();
                return newInstance(typedList);
            }
        }
        return super.convertFrom(representation);
    }

    @Override
    public String toString() {
        if (getRepresentation() instanceof Collection<?> collection) return this.getClass().getName() + "=" + collectionToString(collection);
        return super.toString();
    }

    @SuppressWarnings("unchecked")
    public List<T> toList() {
        if (getRepresentation() instanceof List<?> list) {
            return (List<T>) list;
        }
        return List.of((T) getRepresentation());
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
}

