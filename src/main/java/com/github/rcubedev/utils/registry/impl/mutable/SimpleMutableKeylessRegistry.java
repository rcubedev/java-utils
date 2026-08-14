package com.github.rcubedev.utils.registry.impl.mutable;

import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;
import com.github.rcubedev.utils.registry.api.exception.RegistryInterruptedException;
import com.github.rcubedev.utils.registry.api.mutable.MutableKeylessRegistry;
import com.github.rcubedev.utils.registry.impl.BoundedSnapshotList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SimpleMutableKeylessRegistry<T> implements MutableKeylessRegistry<T> {

    private enum State {
        MUTABLE, FREEZING, FROZEN;
    }

    private final String name;
    private final ConcurrentMap<Integer, T> idToEntryMap = new ConcurrentHashMap<>();

    private final AtomicInteger nextId = new AtomicInteger(0);
    private final AtomicInteger publishedSize = new AtomicInteger(0);

    private final AtomicReference<State> state = new AtomicReference<>(State.MUTABLE);
    private final CountDownLatch freezeLatch = new CountDownLatch(1);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile T[] idToValueArray = null;
    private volatile List<T> cachedEntries = null;

    public SimpleMutableKeylessRegistry(@NotNull String name) {
        this.name = name;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public int register(@NotNull T entry) {
        if (state.get() != State.MUTABLE) throw new RegistryFrozenException(name);

        lock.readLock().lock();
        try {
            if (state.get() != State.MUTABLE) throw new RegistryFrozenException(name);

            int assignedId = nextId.getAndIncrement();
            this.idToEntryMap.put(assignedId, entry);

            advancePublishedSize();
            return assignedId;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void advancePublishedSize() {
        while (true) {
            int current = publishedSize.get();
            if (!idToEntryMap.containsKey(current)) break;
            publishedSize.compareAndSet(current, current + 1);
        }
    }

    @Override
    public @NotNull Optional<T> get(int id) {
        if (state.get() == State.MUTABLE) {
            T val = this.idToEntryMap.get(id);
            if (val != null) return Optional.of(val);
            if (state.get() == State.MUTABLE) return Optional.empty();
        }
        awaitFreeze();

        T[] array = this.idToValueArray;
        if (id >= 0 && id < array.length) {
            return Optional.ofNullable(array[id]);
        }
        return Optional.empty();
    }

    @Override
    public void freeze() {
        if (!this.state.compareAndSet(State.MUTABLE, State.FREEZING)) throw new RegistryFrozenException(name);

        lock.writeLock().lock();
        try {
            try {
                int size = idToEntryMap.size();
                @SuppressWarnings("unchecked")
                T[] idArray = (T[]) new Object[size];

                idToEntryMap.forEach((id, entry) -> {
                    if (id >= 0 && id < size) idArray[id] = entry;
                });

                this.idToValueArray = idArray;
                this.cachedEntries = List.copyOf(Arrays.asList(idArray));

                this.state.set(State.FROZEN);

                this.idToEntryMap.clear();
            } finally {
                this.freezeLatch.countDown();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public @NotNull @Unmodifiable List<T> entries() {
        if (state.get() == State.MUTABLE) {
            int capacity = publishedSize.get();
            BoundedSnapshotList<T> snapshotList = new BoundedSnapshotList<>(this.idToEntryMap, capacity);
            if (state.get() == State.MUTABLE) return snapshotList;
        }
        awaitFreeze();
        return this.cachedEntries;
    }

    private void awaitFreeze() {
        if (state.get() == State.FROZEN) return;
        try {
            this.freezeLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegistryInterruptedException(name, e);
        }
    }
}