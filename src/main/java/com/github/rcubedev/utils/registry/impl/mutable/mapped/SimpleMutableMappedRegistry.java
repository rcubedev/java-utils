package com.github.rcubedev.utils.registry.impl.mutable.mapped;

import com.github.rcubedev.utils.registry.api.exception.RegistryInterruptedException;
import com.github.rcubedev.utils.registry.api.mutable.mapped.MutableMappedRegistry;
import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;
import com.github.rcubedev.utils.registry.api.exception.RegistryNotFrozenException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Deprecated
public final class SimpleMutableMappedRegistry<K, V> implements MutableMappedRegistry<K, V> {

    private enum State {
        MUTABLE, FREEZING, FROZEN;
    }

    private final String name;
    private final ConcurrentMap<K, V> entriesMap = new ConcurrentHashMap<>();
    private final Queue<K> orderedQueue = new ConcurrentLinkedQueue<>(); // ins order
    private final AtomicReference<State> state = new AtomicReference<>(State.MUTABLE);
    private final CountDownLatch freezeLatch = new CountDownLatch(1);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile Map<K, V> cachedSortedMap = null;
    private volatile List<V> cachedEntries = null;

    public SimpleMutableMappedRegistry(@NotNull String name) {
        this.name = name;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    /**
     * Returns the entry for the provided key.<br>
     * Blocks if {@link #freeze()} is currently in progress.
     */
    @Override
    public @NotNull Optional<V> get(@NotNull K key) {
        if (state.get() == State.MUTABLE) {
            V val = this.entriesMap.get(key);
            if (val != null) return Optional.of(val);
            if (state.get() == State.MUTABLE) return Optional.empty();
        }
        awaitFreeze();

        return Optional.ofNullable(this.cachedSortedMap.get(key));
    }

    @Override
    public void register(@NotNull K key, @NotNull V entry) {
        if (state.get() != State.MUTABLE) throw new RegistryFrozenException(name);
        lock.readLock().lock();
        try {
            if (state.get() != State.MUTABLE) throw new RegistryFrozenException(name);
            if (this.entriesMap.putIfAbsent(key, entry) != null)
                throw new IllegalArgumentException("Key " + key + " already registered in registry '" + name + "'");
            this.orderedQueue.add(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void freeze() {
        if (!this.state.compareAndSet(State.MUTABLE, State.FREEZING)) throw new RegistryFrozenException(name);
        lock.writeLock().lock();
        try {
            try {
                Map<K, V> orderedMap = new LinkedHashMap<>(entriesMap.size());
                for (K key : orderedQueue) {
                    V value = entriesMap.get(key);
                    if (value == null) continue;
                    orderedMap.put(key, value);
                }

                this.cachedSortedMap = Collections.unmodifiableMap(orderedMap);
                this.cachedEntries = List.copyOf(orderedMap.values());
                this.state.set(State.FROZEN);

                this.entriesMap.clear();
                this.orderedQueue.clear();
            } finally {
                this.freezeLatch.countDown();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns all entries.<br>
     * Blocks if {@link #freeze()} is currently in progress.
     *
     * @throws RegistryNotFrozenException if called before {@link #freeze()} is triggered
     */
    @Override
    public @NotNull @Unmodifiable Map<K, V> entryMap() {
        if (state.get() == State.MUTABLE) {
            Map<K, V> liveView = Collections.unmodifiableMap(this.entriesMap); // fixme must be frozen view
            if (state.get() == State.MUTABLE) return liveView;
        }
        awaitFreeze();
        return this.cachedSortedMap;
    }

    /**
     * Returns all entries.<br>
     * Blocks if {@link #freeze()} is currently in progress.
     */
    @Override
    public @NotNull @Unmodifiable List<V> entries() {
        if (state.get() == State.MUTABLE) {
            List<V> list = new ArrayList<>(entriesMap.size());
            for (K key : orderedQueue) {
                V val = entriesMap.get(key);
                if (val != null) list.add(val);
            }
            List<V> snapshotList = Collections.unmodifiableList(list);
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
