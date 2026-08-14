package com.github.rcubedev.utils.registry.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A read only registry holding entries of type {@link T}
 *
 * @param <T> the entry type
 */
public interface Registry<T> extends Iterable<T> {

    /**
     * The name of this registry
     */
    @NotNull String name();

    /**
     * Returns an unmodifiable list of all registered entries.
     */
    @NotNull @Unmodifiable List<T> entries();

    /**
     * Returns the total number of registered entries.
     */
    default int size() {
        return entries().size();
    }

    /**
     * Returns true if no entries have been registered.
     */
    default boolean isEmpty() {
        return entries().isEmpty();
    }

    /**
     * Returns a sequential {@link Stream} over the registered entries.
     */
    default Stream<T> stream() {
        return entries().stream();
    }

    @Override
    default @NotNull Iterator<T> iterator() {
        return entries().iterator();
    }
}
