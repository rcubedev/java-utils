package com.github.rcubedev.utils.registry.impl.mutable.mapped;

import com.github.rcubedev.utils.registry.api.exception.RegistryInterruptedException;
import com.github.rcubedev.utils.registry.api.mutable.mapped.MutableMappedIdRegistry;
import com.github.rcubedev.utils.registry.api.exception.RegistryFrozenException;
import com.github.rcubedev.utils.registry.impl.BoundedSnapshotList;
import com.github.rcubedev.utils.registry.impl.BoundedSnapshotMap;
import com.github.rcubedev.utils.registry.impl.EntryHolder;
import com.github.rcubedev.utils.util.impl.TransformingMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SimpleMutableMappedIdRegistry<K, V> implements MutableMappedIdRegistry<K, V> {

    private enum State {
        MUTABLE, FREEZING, FROZEN;
    }

    private final String name;
    private final ConcurrentMap<K, EntryHolder<K, V>> keyToEntryMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, EntryHolder<K, V>> idToEntryMap = new ConcurrentHashMap<>();

    private final AtomicInteger nextId = new AtomicInteger(0);
    private final AtomicInteger publishedSize = new AtomicInteger(0);

    private final AtomicReference<State> state = new AtomicReference<>(State.MUTABLE);
    private final CountDownLatch freezeLatch = new CountDownLatch(1);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile V[] idToValueArray = null;
    private volatile Map<K, V> cachedKeyToValue = null;
    private volatile Map<K, Integer> cachedKeyToId = null;
    private volatile List<V> cachedEntries = null;

    public SimpleMutableMappedIdRegistry(@NotNull String name) {
        this.name = name;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public int registerId(@NotNull K key, @NotNull V entry) {
        if (state.get() != State.MUTABLE) throw new RegistryFrozenException(name);

        lock.readLock().lock();
        try {
            if (state.get() != State.MUTABLE) throw new RegistryFrozenException(name);

            int assignedId = nextId.getAndIncrement();
            EntryHolder<K, V> holder = new EntryHolder<>(assignedId, key, entry);

            if (this.keyToEntryMap.putIfAbsent(key, holder) != null)
                throw new IllegalArgumentException("Key " + key + " already registered in registry '" + name + "'");
            this.idToEntryMap.put(assignedId, holder);

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
    public @NotNull Optional<V> get(@NotNull K key) {
        if (state.get() == State.MUTABLE) {
            EntryHolder<K, V> holder = this.keyToEntryMap.get(key);
            if (holder != null) return Optional.of(holder.value());
            if (state.get() == State.MUTABLE) return Optional.empty();
        }
        awaitFreeze();

        return Optional.ofNullable(this.cachedKeyToValue.get(key));
    }

    @Override
    public @NotNull Optional<V> getById(int id) {
        if (state.get() == State.MUTABLE) {
            EntryHolder<K, V> holder = this.idToEntryMap.get(id);
            if (holder != null) return Optional.of(holder.value());
            if (state.get() == State.MUTABLE) return Optional.empty();
        }
        awaitFreeze();

        V[] array = this.idToValueArray;
        if (id >= 0 && id < array.length) {
            return Optional.ofNullable(array[id]);
        }
        return Optional.empty();
    }

    @Override
    public int getId(@NotNull K key) {
        if (state.get() == State.MUTABLE) {
            EntryHolder<K, V> holder = this.keyToEntryMap.get(key);
            if (holder != null) return holder.id();
            if (state.get() == State.MUTABLE) return Integer.MIN_VALUE;
        }
        awaitFreeze();

        Integer id = this.cachedKeyToId.get(key);
        return id != null ? id : Integer.MIN_VALUE;
    }

    @Override
    public void freeze() {
        if (!this.state.compareAndSet(State.MUTABLE, State.FREEZING)) throw new RegistryFrozenException(name);

        lock.writeLock().lock();
        try {
            try {
                int size = keyToEntryMap.size();
                Map<K, V> keyToValMap = new LinkedHashMap<>(size);
                Map<K, Integer> keyToIdMap = new LinkedHashMap<>(size);
                @SuppressWarnings("unchecked")

                V[] idArray = (V[]) new Object[size];

                keyToEntryMap.values().stream().sorted(Comparator.comparingInt(EntryHolder::id)).forEach(holder -> {
                    keyToValMap.put(holder.key(), holder.value());
                    keyToIdMap.put(holder.key(), holder.id());
                    idArray[holder.id()] = holder.value();
                });

                this.idToValueArray = idArray;
                this.cachedKeyToValue = Collections.unmodifiableMap(keyToValMap);
                this.cachedKeyToId = Collections.unmodifiableMap(keyToIdMap);
                this.cachedEntries = List.copyOf(keyToValMap.values());

                this.state.set(State.FROZEN);

                this.keyToEntryMap.clear();
                this.idToEntryMap.clear();
            } finally {
                this.freezeLatch.countDown();
            }

        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public @NotNull @Unmodifiable Map<K, V> entryMap() {
        if (state.get() == State.MUTABLE) {
            int capacity = publishedSize.get();
            BoundedSnapshotList<EntryHolder<K, V>> holderList = new BoundedSnapshotList<>(this.idToEntryMap, capacity);
            BoundedSnapshotMap<K, V> snapshotMap = new BoundedSnapshotMap<>(holderList, this.keyToEntryMap);
            if (state.get() == State.MUTABLE) return snapshotMap;
        }

        awaitFreeze();
        return this.cachedKeyToValue;
    }

    @Override
    public @NotNull @Unmodifiable List<V> entries() {
        if (state.get() == State.MUTABLE) {
            int capacity = publishedSize.get();
            Map<Integer, V> liveView = new TransformingMap<>(this.idToEntryMap, EntryHolder::value);
            BoundedSnapshotList<V> snapshotList = new BoundedSnapshotList<>(liveView, capacity);
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

