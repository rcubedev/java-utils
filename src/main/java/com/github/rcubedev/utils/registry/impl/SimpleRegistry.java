package com.github.rcubedev.utils.registry.impl;

import com.github.rcubedev.utils.registry.api.Registry;
import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Standard {@link Registry} implementation.
 */
public final class SimpleRegistry<T> implements Registry<T> {

    private final String name;
    private final List<T> entries = new ArrayList<>();
    private final Object lock = new Object();

    private volatile boolean frozen = false;

    public SimpleRegistry(@NotNull String name) {
        this.name = name;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public void register(@NotNull T entry) {
        mutate(t -> t.entries.add(entry));
    }

    @Override
    public void freeze() {
        if (this.frozen) throw new RegistryFrozenException(name);
        synchronized (lock) {
            if (this.frozen) throw new RegistryFrozenException(name);
            this.frozen = true;
        }
    }

    /**
     * Returns all entries.
     *
     * @throws RegistryFrozenException if not yet frozen
     */
    @Override
    public @NotNull @Unmodifiable List<T> entries() {
        if (!frozen) throw new RegistryFrozenException(name);
        return Collections.unmodifiableList(entries);
    }

    private void mutate(Consumer<SimpleRegistry<T>> consumer) {
        if (this.frozen) throw new RegistryFrozenException(name);
        synchronized (lock) {
            if (this.frozen) throw new RegistryFrozenException(name);
            consumer.accept(this);
        }
    }
}