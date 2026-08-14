package com.github.rcubedev.utils.util.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Lazy read only view of a {@link Map}<br>
 * Applies a transformation function to values on demand.
 */
public final class TransformingMap<K, V1, V> extends AbstractMap<K, V> {

    private static final Object NULL_SENTINEL = new Object();

    private final Map<K, V1> backingMap;
    private final Function<? super V1, ? extends V> valueTransformer;

    public TransformingMap(@NotNull Map<K, V1> backingMap,
                           @NotNull Function<? super V1, ? extends V> valueTransformer) {
        this.backingMap = Objects.requireNonNull(backingMap, "backingMap");
        this.valueTransformer = Objects.requireNonNull(valueTransformer, "valueTransformer");
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable V get(Object key) {
        Object result = backingMap.getOrDefault(key, (V1) NULL_SENTINEL); // safe due to erasure as long as maps don't check #getOrDefault

        if (result == NULL_SENTINEL) return null;
        return valueTransformer.apply((V1) result); // safe as null sentinel only Object, all else correct
    }

    @Override
    public boolean containsKey(Object key) {
        return backingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public @NotNull Set<K> keySet() {
        return backingMap.keySet();
    }

    @Override
    public @NotNull Collection<V> values() {
        return new AbstractCollection<>() {

            @Override
            public int size() {
                return TransformingMap.this.size();
            }

            @Override
            public boolean isEmpty() {
                return TransformingMap.this.isEmpty();
            }

            @Override
            public @NotNull Iterator<V> iterator() {
                Iterator<V1> iterator = backingMap.values().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public V next() {
                        return valueTransformer.apply(iterator.next());
                    }
                };
            }
        };
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return new TransformedEntrySet();
    }

    private final class TransformedEntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public int size() {
            return backingMap.size();
        }

        @Override
        public boolean isEmpty() {
            return backingMap.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) return false;
            Object key = entry.getKey();
            if (!containsKey(key)) return false;
            return Objects.equals(get(key), entry.getValue());
        }

        @Override
        public @NotNull Iterator<Entry<K, V>> iterator() {
            Iterator<Entry<K, V1>> iterator = backingMap.entrySet().iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Entry<K, V> next() {
                    Entry<K, V1> entry = iterator.next();
                    return new SimpleImmutableEntry<>(entry.getKey(), valueTransformer.apply(entry.getValue()));
                }
            };
        }
    }
}
