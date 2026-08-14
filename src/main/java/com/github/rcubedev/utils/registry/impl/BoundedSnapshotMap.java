package com.github.rcubedev.utils.registry.impl;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class BoundedSnapshotMap<K, V> extends AbstractMap<K, V> {
    private final BoundedSnapshotList<EntryHolder<K, V>> list;
    private final Map<K, EntryHolder<K, V>> keyMap;

    public BoundedSnapshotMap(BoundedSnapshotList<EntryHolder<K, V>> list, Map<K, EntryHolder<K, V>> keyMap) {
        this.list = list;
        this.keyMap = keyMap;
    }

    @Override
    public V get(Object key) {
        EntryHolder<K, V> holder = keyMap.get(key);
        return (holder != null && holder.id() < list.size()) ? holder.value() : null;
    }

    @Override
    public boolean containsKey(Object key) {
        //noinspection SuspiciousMethodCalls
        EntryHolder<K, V> holder = keyMap.get(key);
        return holder != null && holder.id() < list.size();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public int size() {
            return list.size();
        }

        @Override
        public @NotNull Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            //noinspection SuspiciousMethodCalls
            EntryHolder<K, V> candidate = keyMap.get(e.getKey());
            return candidate != null && candidate.id() < list.size() && Objects.equals(candidate.value(), e.getValue());
        }
    }

    private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < list.size();
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) throw new NoSuchElementException();
            EntryHolder<K, V> holder = list.get(index++);
            return new SimpleImmutableEntry<>(holder.key(), holder.value());
        }
    }
}
